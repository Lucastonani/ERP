package com.erp.ia.execution.handler;

import com.erp.ia.agent.model.ActionType;
import com.erp.ia.agent.model.PlannedAction;
import com.erp.ia.core.model.Product;
import com.erp.ia.core.model.Stock;
import com.erp.ia.core.model.StockMovement;
import com.erp.ia.core.repository.ProductRepository;
import com.erp.ia.core.service.StockService;
import com.erp.ia.event.EventBus;
import com.erp.ia.event.model.StockLevelChanged;
import com.erp.ia.execution.ActionHandler;
import com.erp.ia.execution.ExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Handles ADJUST_STOCK actions.
 * Extracts productId, warehouse, quantity, movementType, reason from params,
 * delegates to StockService.adjustStock(), and publishes StockLevelChanged
 * event.
 */
@Component
public class AdjustStockHandler implements ActionHandler {

    private static final Logger log = LoggerFactory.getLogger(AdjustStockHandler.class);

    private final StockService stockService;
    private final ProductRepository productRepository;
    private final EventBus eventBus;

    public AdjustStockHandler(StockService stockService, ProductRepository productRepository, EventBus eventBus) {
        this.stockService = stockService;
        this.productRepository = productRepository;
        this.eventBus = eventBus;
    }

    @Override
    public ActionType getActionType() {
        return ActionType.ADJUST_STOCK;
    }

    @Override
    public ExecutionResult handle(PlannedAction action, String auditId, String executedBy) {
        Map<String, Object> params = action.getParams();

        Long productId = getLongParam(params, "productId");
        if (productId == null) {
            return ExecutionResult.failed(ActionType.ADJUST_STOCK.name(),
                    "Parâmetro obrigatório ausente: productId");
        }

        String warehouse = getStringParam(params, "warehouse", "MAIN");
        BigDecimal quantity = getBigDecimalParam(params, "quantity");
        if (quantity == null) {
            return ExecutionResult.failed(ActionType.ADJUST_STOCK.name(),
                    "Parâmetro obrigatório ausente: quantity");
        }

        String movementTypeStr = getStringParam(params, "movementType", "ADJUST");
        StockMovement.MovementType movementType;
        try {
            movementType = StockMovement.MovementType.valueOf(movementTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ExecutionResult.failed(ActionType.ADJUST_STOCK.name(),
                    "Tipo de movimento inválido: " + movementTypeStr);
        }

        String reason = getStringParam(params, "reason", "Ajuste via agente IA | auditId=" + auditId);

        log.info("Adjusting stock: productId={}, warehouse={}, type={}, qty={}, auditId={}",
                productId, warehouse, movementType, quantity, auditId);

        try {
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado: " + productId));

            // Capture previous quantity for event
            BigDecimal previousQuantity = stockService.getStock(productId, warehouse)
                    .map(Stock::getQuantity)
                    .orElse(BigDecimal.ZERO);

            StockMovement movement = stockService.adjustStock(
                    product, warehouse, movementType, quantity, reason, executedBy);

            // Calculate new quantity for event
            BigDecimal newQuantity = stockService.getStock(productId, warehouse)
                    .map(Stock::getQuantity)
                    .orElse(BigDecimal.ZERO);

            log.info("Stock adjusted: movementId={}, previous={}, new={}",
                    movement.getId(), previousQuantity, newQuantity);

            // Publish domain event
            eventBus.publish(new StockLevelChanged(
                    productId, warehouse, previousQuantity, newQuantity, reason));

            return ExecutionResult.success(
                    ActionType.ADJUST_STOCK.name(),
                    "Estoque ajustado com sucesso para o produto " + productId,
                    Map.of(
                            "movementId", movement.getId(),
                            "productId", productId,
                            "warehouse", warehouse,
                            "previousQuantity", previousQuantity,
                            "newQuantity", newQuantity,
                            "movementType", movementType.name()));

        } catch (Exception e) {
            log.error("Failed to adjust stock: {}", e.getMessage(), e);
            return ExecutionResult.failed(ActionType.ADJUST_STOCK.name(),
                    "Falha ao ajustar estoque: " + e.getMessage());
        }
    }

    private String getStringParam(Map<String, Object> params, String key, String defaultValue) {
        if (params == null || !params.containsKey(key)) {
            return defaultValue;
        }
        Object value = params.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    private Long getLongParam(Map<String, Object> params, String key) {
        if (params == null || !params.containsKey(key)) {
            return null;
        }
        Object value = params.get(key);
        if (value instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private BigDecimal getBigDecimalParam(Map<String, Object> params, String key) {
        if (params == null || !params.containsKey(key)) {
            return null;
        }
        Object value = params.get(key);
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        if (value instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
