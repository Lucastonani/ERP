package com.erp.ia.core.service;

import com.erp.ia.core.model.PurchaseOrder;
import com.erp.ia.core.model.PurchaseOrderItem;
import com.erp.ia.core.repository.PurchaseOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PurchaseOrderService {

    private final PurchaseOrderRepository orderRepository;

    public PurchaseOrderService(PurchaseOrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional
    public PurchaseOrder createDraft(String supplier, List<PurchaseOrderItem> items, String createdBy,
            String tenantId, String storeId) {
        PurchaseOrder order = new PurchaseOrder("PO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                supplier);
        order.setCreatedBy(createdBy);
        order.setTenantId(tenantId);
        order.setStoreId(storeId);
        items.forEach(order::addItem);
        return orderRepository.save(order);
    }

    @Transactional
    public PurchaseOrder approve(Long orderId, String approvedBy) {
        PurchaseOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        if (order.getStatus() != PurchaseOrder.OrderStatus.DRAFT &&
                order.getStatus() != PurchaseOrder.OrderStatus.PENDING) {
            throw new IllegalStateException("Cannot approve order in status: " + order.getStatus());
        }
        order.setStatus(PurchaseOrder.OrderStatus.APPROVED);
        order.setApprovedBy(approvedBy);
        return orderRepository.save(order);
    }

    public Optional<PurchaseOrder> findById(Long id) {
        return orderRepository.findById(id);
    }

    public List<PurchaseOrder> findByStatus(PurchaseOrder.OrderStatus status) {
        return orderRepository.findByStatus(status);
    }
}
