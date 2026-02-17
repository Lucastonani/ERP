package com.erp.ia.core.service;

import com.erp.ia.core.model.Product;
import com.erp.ia.core.model.Stock;
import com.erp.ia.core.model.StockMovement;
import com.erp.ia.core.repository.StockMovementRepository;
import com.erp.ia.core.repository.StockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class StockService {

    private final StockRepository stockRepository;
    private final StockMovementRepository movementRepository;

    public StockService(StockRepository stockRepository, StockMovementRepository movementRepository) {
        this.stockRepository = stockRepository;
        this.movementRepository = movementRepository;
    }

    public Optional<Stock> getStock(Long productId, String warehouse) {
        return stockRepository.findByProductIdAndWarehouse(productId, warehouse);
    }

    public List<Stock> getStocksByProduct(Long productId) {
        return stockRepository.findByProductId(productId);
    }

    @Transactional
    public StockMovement adjustStock(Product product, String warehouse, StockMovement.MovementType type,
            BigDecimal quantity, String reason, String createdBy) {
        Stock stock = stockRepository.findByProductIdAndWarehouse(product.getId(), warehouse)
                .orElseGet(() -> {
                    Stock s = new Stock(product, warehouse, BigDecimal.ZERO);
                    return stockRepository.save(s);
                });

        switch (type) {
            case IN -> stock.setQuantity(stock.getQuantity().add(quantity));
            case OUT -> stock.setQuantity(stock.getQuantity().subtract(quantity));
            case ADJUST -> stock.setQuantity(quantity);
        }

        stockRepository.save(stock);

        StockMovement movement = new StockMovement(product, type, quantity, reason);
        movement.setWarehouse(warehouse);
        movement.setCreatedBy(createdBy);
        return movementRepository.save(movement);
    }

    public List<Stock> findBelowMinimum(String tenantId) {
        return stockRepository.findByTenantId(tenantId).stream()
                .filter(Stock::isBelowMinimum)
                .toList();
    }
}
