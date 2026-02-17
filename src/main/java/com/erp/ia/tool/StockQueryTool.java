package com.erp.ia.tool;

import com.erp.ia.core.model.Stock;
import com.erp.ia.core.repository.StockRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class StockQueryTool implements AgentTool<StockQueryTool.Input, StockQueryTool.Output> {

    private final StockRepository stockRepository;

    public StockQueryTool(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    @Override
    public String getName() {
        return "StockQueryTool";
    }

    @Override
    public String getDescription() {
        return "Queries current stock levels by product or warehouse";
    }

    @Override
    public Class<Input> getInputType() {
        return Input.class;
    }

    @Override
    public Class<Output> getOutputType() {
        return Output.class;
    }

    @Override
    public Output execute(Input input) {
        List<Stock> stocks;
        if (input.productId != null) {
            stocks = stockRepository.findByProductId(input.productId);
        } else if (input.warehouse != null) {
            stocks = stockRepository.findByWarehouse(input.warehouse);
        } else {
            stocks = stockRepository.findByTenantId(input.tenantId != null ? input.tenantId : "default");
        }

        List<StockItem> items = stocks.stream()
                .map(s -> new StockItem(
                        s.getProduct().getId(), s.getProduct().getSku(), s.getProduct().getName(),
                        s.getWarehouse(), s.getQuantity(), s.getMinQuantity(), s.isBelowMinimum()))
                .toList();

        return new Output(items, items.stream().anyMatch(StockItem::belowMinimum));
    }

    // --- Typed DTOs ---

    public static class Input {
        public Long productId;
        public String warehouse;
        public String tenantId;
    }

    public record Output(List<StockItem> stocks, boolean hasItemsBelowMinimum) {
    }

    public record StockItem(
            Long productId, String sku, String name, String warehouse,
            BigDecimal quantity, BigDecimal minQuantity, boolean belowMinimum) {
    }
}
