package com.erp.ia.core.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "stocks", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "product_id", "warehouse", "tenant_id" })
})
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, length = 100)
    private String warehouse = "MAIN";

    @Column(nullable = false, precision = 15, scale = 4)
    private BigDecimal quantity = BigDecimal.ZERO;

    @Column(name = "min_quantity", nullable = false, precision = 15, scale = 4)
    private BigDecimal minQuantity = BigDecimal.ZERO;

    @Column(name = "max_quantity", precision = 15, scale = 4)
    private BigDecimal maxQuantity;

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId = "default";

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Stock() {
    }

    public Stock(Product product, String warehouse, BigDecimal quantity) {
        this.product = product;
        this.warehouse = warehouse;
        this.quantity = quantity;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // --- Getters & Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public String getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(String warehouse) {
        this.warehouse = warehouse;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getMinQuantity() {
        return minQuantity;
    }

    public void setMinQuantity(BigDecimal minQuantity) {
        this.minQuantity = minQuantity;
    }

    public BigDecimal getMaxQuantity() {
        return maxQuantity;
    }

    public void setMaxQuantity(BigDecimal maxQuantity) {
        this.maxQuantity = maxQuantity;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public boolean isBelowMinimum() {
        return quantity.compareTo(minQuantity) < 0;
    }
}
