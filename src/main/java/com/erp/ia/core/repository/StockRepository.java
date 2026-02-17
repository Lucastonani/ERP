package com.erp.ia.core.repository;

import com.erp.ia.core.model.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long> {
    Optional<Stock> findByProductIdAndWarehouse(Long productId, String warehouse);

    List<Stock> findByProductId(Long productId);

    List<Stock> findByWarehouse(String warehouse);

    List<Stock> findByTenantId(String tenantId);
}
