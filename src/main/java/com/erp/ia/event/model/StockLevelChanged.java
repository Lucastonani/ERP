package com.erp.ia.event.model;

import com.erp.ia.event.DomainEvent;
import java.math.BigDecimal;

public class StockLevelChanged extends DomainEvent {

    private final Long productId;
    private final String warehouse;
    private final BigDecimal previousQuantity;
    private final BigDecimal newQuantity;
    private final String reason;

    public StockLevelChanged(Long productId, String warehouse,
            BigDecimal previousQuantity, BigDecimal newQuantity, String reason) {
        super("STOCK_LEVEL_CHANGED", 1);
        this.productId = productId;
        this.warehouse = warehouse;
        this.previousQuantity = previousQuantity;
        this.newQuantity = newQuantity;
        this.reason = reason;
    }

    public Long getProductId() {
        return productId;
    }

    public String getWarehouse() {
        return warehouse;
    }

    public BigDecimal getPreviousQuantity() {
        return previousQuantity;
    }

    public BigDecimal getNewQuantity() {
        return newQuantity;
    }

    public String getReason() {
        return reason;
    }
}
