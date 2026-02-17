package com.erp.ia.core.repository;

import com.erp.ia.core.model.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    Optional<PurchaseOrder> findByOrderNumber(String orderNumber);

    List<PurchaseOrder> findByStatus(PurchaseOrder.OrderStatus status);

    List<PurchaseOrder> findByTenantId(String tenantId);
}
