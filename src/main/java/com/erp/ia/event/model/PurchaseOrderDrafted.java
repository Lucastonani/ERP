package com.erp.ia.event.model;

import com.erp.ia.event.DomainEvent;

public class PurchaseOrderDrafted extends DomainEvent {

    private final Long orderId;
    private final String orderNumber;
    private final String supplier;
    private final String auditId;

    public PurchaseOrderDrafted(Long orderId, String orderNumber, String supplier, String auditId) {
        super("PURCHASE_ORDER_DRAFTED", 1);
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.supplier = supplier;
        this.auditId = auditId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public String getSupplier() {
        return supplier;
    }

    public String getAuditId() {
        return auditId;
    }
}
