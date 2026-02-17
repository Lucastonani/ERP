package com.erp.ia.core;

import com.erp.ia.core.model.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class CoreModelTest {

    @Test
    void stockShouldDetectBelowMinimum() {
        Stock stock = new Stock();
        stock.setQuantity(BigDecimal.valueOf(3));
        stock.setMinQuantity(BigDecimal.TEN);
        assertTrue(stock.isBelowMinimum());
    }

    @Test
    void stockShouldDetectAboveMinimum() {
        Stock stock = new Stock();
        stock.setQuantity(BigDecimal.valueOf(15));
        stock.setMinQuantity(BigDecimal.TEN);
        assertFalse(stock.isBelowMinimum());
    }

    @Test
    void purchaseOrderShouldRecalculateTotal() {
        PurchaseOrder po = new PurchaseOrder();
        Product p1 = new Product();
        p1.setId(1L);

        PurchaseOrderItem item = new PurchaseOrderItem();
        item.setProduct(p1);
        item.setQuantity(BigDecimal.valueOf(10));
        item.setUnitPrice(BigDecimal.valueOf(5.50));
        po.getItems().add(item);

        po.recalculateTotal();

        assertEquals(BigDecimal.valueOf(55.00).setScale(2), po.getTotal().setScale(2));
    }

    @Test
    void purchaseOrderItemShouldComputeTotalPrice() {
        PurchaseOrderItem item = new PurchaseOrderItem();
        item.setQuantity(BigDecimal.valueOf(3));
        item.setUnitPrice(BigDecimal.valueOf(10));
        assertEquals(BigDecimal.valueOf(30), item.getTotalPrice());
    }

    @Test
    void productShouldDefaultToActive() {
        Product product = new Product();
        assertTrue(product.isActive());
    }

    @Test
    void purchaseOrderShouldStartAsDraft() {
        PurchaseOrder po = new PurchaseOrder();
        assertEquals(PurchaseOrder.OrderStatus.DRAFT, po.getStatus());
    }
}
