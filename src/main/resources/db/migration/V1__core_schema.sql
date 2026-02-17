-- =============================================
-- V1: Core schema — transactional ERP entities
-- =============================================

CREATE TABLE products (
    id              BIGSERIAL       PRIMARY KEY,
    sku             VARCHAR(50)     NOT NULL UNIQUE,
    name            VARCHAR(255)    NOT NULL,
    description     TEXT,
    category        VARCHAR(100),
    unit            VARCHAR(20)     NOT NULL DEFAULT 'UN',
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    tenant_id       VARCHAR(50)     NOT NULL DEFAULT 'default',
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE stocks (
    id              BIGSERIAL       PRIMARY KEY,
    product_id      BIGINT          NOT NULL REFERENCES products(id),
    warehouse       VARCHAR(100)    NOT NULL DEFAULT 'MAIN',
    quantity         DECIMAL(15,4)   NOT NULL DEFAULT 0,
    min_quantity     DECIMAL(15,4)   NOT NULL DEFAULT 0,
    max_quantity     DECIMAL(15,4),
    tenant_id       VARCHAR(50)     NOT NULL DEFAULT 'default',
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(product_id, warehouse, tenant_id)
);

CREATE TABLE stock_movements (
    id              BIGSERIAL       PRIMARY KEY,
    product_id      BIGINT          NOT NULL REFERENCES products(id),
    warehouse       VARCHAR(100)    NOT NULL DEFAULT 'MAIN',
    movement_type   VARCHAR(20)     NOT NULL, -- IN, OUT, ADJUST
    quantity         DECIMAL(15,4)   NOT NULL,
    reason          VARCHAR(500),
    reference_id    VARCHAR(100),
    tenant_id       VARCHAR(50)     NOT NULL DEFAULT 'default',
    created_by      VARCHAR(100),
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE purchase_orders (
    id              BIGSERIAL       PRIMARY KEY,
    order_number    VARCHAR(50)     NOT NULL UNIQUE,
    supplier        VARCHAR(255)    NOT NULL,
    status          VARCHAR(30)     NOT NULL DEFAULT 'DRAFT', -- DRAFT, PENDING, APPROVED, COMPLETED, CANCELLED
    total           DECIMAL(15,2)   NOT NULL DEFAULT 0,
    notes           TEXT,
    tenant_id       VARCHAR(50)     NOT NULL DEFAULT 'default',
    store_id        VARCHAR(50)     NOT NULL DEFAULT 'default',
    created_by      VARCHAR(100),
    approved_by     VARCHAR(100),
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE purchase_order_items (
    id              BIGSERIAL       PRIMARY KEY,
    order_id        BIGINT          NOT NULL REFERENCES purchase_orders(id) ON DELETE CASCADE,
    product_id      BIGINT          NOT NULL REFERENCES products(id),
    quantity         DECIMAL(15,4)   NOT NULL,
    unit_price      DECIMAL(15,4)   NOT NULL,
    total_price     DECIMAL(15,2)
);

CREATE INDEX idx_products_sku ON products(sku);
CREATE INDEX idx_products_tenant ON products(tenant_id);
CREATE INDEX idx_stocks_product ON stocks(product_id);
CREATE INDEX idx_stock_movements_product ON stock_movements(product_id);
CREATE INDEX idx_stock_movements_created ON stock_movements(created_at);
CREATE INDEX idx_purchase_orders_status ON purchase_orders(status);
CREATE INDEX idx_purchase_orders_tenant ON purchase_orders(tenant_id);
