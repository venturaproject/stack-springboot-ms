-- Example domain: a small e-commerce catalog — Product, Customer, Order (+ OrderLine child).
-- Product and Order are rich aggregates (unique SKU, non-negative stock, derived total,
-- draft-only edits). Notification and AuditLog are plain entities. Money is stored as
-- integer minor units plus an ISO-4217 currency code so arithmetic never hits floating point.

CREATE TABLE products (
    id                       UUID PRIMARY KEY,
    sku                      VARCHAR(64)  NOT NULL UNIQUE,
    name                     VARCHAR(255) NOT NULL,
    description              TEXT,
    price_amount_minor_units BIGINT       NOT NULL,
    price_currency           VARCHAR(3)   NOT NULL,
    stock_quantity           INTEGER      NOT NULL DEFAULT 0,
    image_filename           VARCHAR(255),
    created_at               TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE customers (
    id                  UUID PRIMARY KEY,
    name                VARCHAR(255) NOT NULL,
    email               VARCHAR(254) NOT NULL UNIQUE,
    address_street      VARCHAR(255),
    address_city        VARCHAR(255),
    address_postal_code VARCHAR(32),
    address_country     VARCHAR(255),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE orders (
    id                           UUID PRIMARY KEY,
    customer_id                  UUID NOT NULL REFERENCES customers(id),
    status                       VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    shipping_address_street      VARCHAR(255),
    shipping_address_city        VARCHAR(255),
    shipping_address_postal_code VARCHAR(32),
    shipping_address_country     VARCHAR(255),
    total_amount_minor_units     BIGINT,
    total_currency               VARCHAR(3),
    created_at                   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE order_lines (
    id                           UUID PRIMARY KEY,
    order_id                     UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    line_index                   INTEGER NOT NULL DEFAULT 0,
    product_id                   UUID NOT NULL,
    product_sku                  VARCHAR(64)  NOT NULL,
    product_name                 VARCHAR(255) NOT NULL,
    unit_price_amount_minor_units BIGINT      NOT NULL,
    unit_price_currency          VARCHAR(3)   NOT NULL,
    quantity                     INTEGER      NOT NULL
);

CREATE TABLE notifications (
    id         UUID PRIMARY KEY,
    user_id    UUID,
    title      VARCHAR(255) NOT NULL,
    message    TEXT NOT NULL,
    type       VARCHAR(20) NOT NULL DEFAULT 'info',
    read       BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE audit_logs (
    id         UUID PRIMARY KEY,
    actor_id   VARCHAR(255) NOT NULL,
    action     VARCHAR(50)  NOT NULL,
    entity     VARCHAR(100) NOT NULL,
    entity_id  VARCHAR(255),
    changes    TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_products_created       ON products(created_at DESC);
CREATE INDEX idx_products_stock         ON products(stock_quantity);
CREATE INDEX idx_customers_created      ON customers(created_at DESC);
CREATE INDEX idx_orders_customer        ON orders(customer_id);
CREATE INDEX idx_orders_status          ON orders(status);
CREATE INDEX idx_orders_created         ON orders(created_at DESC);
CREATE INDEX idx_order_lines_order      ON order_lines(order_id);
CREATE INDEX idx_notifications_user     ON notifications(user_id, created_at DESC);
CREATE INDEX idx_audit_logs_actor       ON audit_logs(actor_id, created_at DESC);
CREATE INDEX idx_audit_logs_entity      ON audit_logs(entity, entity_id);
