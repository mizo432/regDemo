-- Product (商品マスター)
CREATE TABLE product (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    barcode VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    price DECIMAL(12, 2) NOT NULL,
    tax_category VARCHAR(20) NOT NULL DEFAULT 'TAXABLE',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_product_barcode ON product(barcode);
CREATE INDEX idx_product_status ON product(status);

-- TimeSale (タイムセール)
CREATE TABLE time_sale (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    sale_price DECIMAL(12, 2) NOT NULL,
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_timesale_product FOREIGN KEY (product_id) REFERENCES product(id)
);

CREATE INDEX idx_timesale_product ON time_sale(product_id);
CREATE INDEX idx_timesale_period ON time_sale(start_date, end_date);

-- SetProduct (セット商品)
CREATE TABLE set_product (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    set_barcode VARCHAR(50) NOT NULL UNIQUE,
    set_name VARCHAR(200) NOT NULL,
    set_price DECIMAL(12, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_setproduct_barcode ON set_product(set_barcode);

-- SetProductItem (セット商品構成)
CREATE TABLE set_product_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    set_product_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_setitem_set FOREIGN KEY (set_product_id) REFERENCES set_product(id),
    CONSTRAINT fk_setitem_product FOREIGN KEY (product_id) REFERENCES product(id)
);

CREATE INDEX idx_setitem_set ON set_product_item(set_product_id);

-- Inventory (在庫)
CREATE TABLE inventory (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL UNIQUE,
    quantity INT NOT NULL DEFAULT 0,
    reserved_quantity INT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_inventory_product FOREIGN KEY (product_id) REFERENCES product(id)
);

CREATE INDEX idx_inventory_product ON inventory(product_id);

-- Transaction (売上取引)
CREATE TABLE transaction (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_id VARCHAR(50) NOT NULL UNIQUE,
    date_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    terminal_id VARCHAR(50),
    operator_id VARCHAR(50),
    subtotal DECIMAL(12, 2) NOT NULL DEFAULT 0,
    tax_total DECIMAL(12, 2) NOT NULL DEFAULT 0,
    grand_total DECIMAL(12, 2) NOT NULL DEFAULT 0,
    payment_method VARCHAR(20),
    paid_amount DECIMAL(12, 2),
    status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_transaction_id ON transaction(transaction_id);
CREATE INDEX idx_transaction_status ON transaction(status);
CREATE INDEX idx_transaction_date ON transaction(date_time);

-- TransactionItem (売上明細)
CREATE TABLE transaction_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(12, 2) NOT NULL,
    tax_amount DECIMAL(12, 2) NOT NULL DEFAULT 0,
    total_amount DECIMAL(12, 2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_item_transaction FOREIGN KEY (transaction_id) REFERENCES transaction(id),
    CONSTRAINT fk_item_product FOREIGN KEY (product_id) REFERENCES product(id)
);

CREATE INDEX idx_item_transaction ON transaction_item(transaction_id);
CREATE INDEX idx_item_product ON transaction_item(product_id);

-- CommissionProduct (委託商品設定)
CREATE TABLE commission_product (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL UNIQUE,
    commissioner VARCHAR(200) NOT NULL,
    commission_rate DECIMAL(5, 4),
    fixed_commission DECIMAL(12, 2),
    commission_type VARCHAR(10) NOT NULL DEFAULT 'RATE',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_commission_product FOREIGN KEY (product_id) REFERENCES product(id)
);

CREATE INDEX idx_commission_product ON commission_product(product_id);

-- CommissionRecord (委託販売記録)
CREATE TABLE commission_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    sale_amount DECIMAL(12, 2) NOT NULL,
    commission_amount DECIMAL(12, 2) NOT NULL,
    payable_amount DECIMAL(12, 2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_record_transaction FOREIGN KEY (transaction_id) REFERENCES transaction(id),
    CONSTRAINT fk_record_product FOREIGN KEY (product_id) REFERENCES product(id)
);

CREATE INDEX idx_commission_record_transaction ON commission_record(transaction_id);

-- CancelReturn (取消・返品)
CREATE TABLE cancel_return (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    original_transaction_id VARCHAR(50) NOT NULL,
    reason VARCHAR(500),
    operator_id VARCHAR(50),
    terminal_id VARCHAR(50),
    date_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_cancel_original ON cancel_return(original_transaction_id);
CREATE INDEX idx_cancel_date ON cancel_return(date_time);

-- SyncOutbox (送信待ち)
CREATE TABLE sync_outbox (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_id VARCHAR(50) NOT NULL UNIQUE,
    payload CLOB NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    last_error VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_outbox_status ON sync_outbox(status);
CREATE INDEX idx_outbox_transaction ON sync_outbox(transaction_id);

-- SyncInbox (受信待ち)
CREATE TABLE sync_inbox (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    event_id VARCHAR(50) NOT NULL,
    payload CLOB NOT NULL,
    applied BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    applied_at TIMESTAMP
);

CREATE INDEX idx_inbox_applied ON sync_inbox(applied);
CREATE INDEX idx_inbox_event ON sync_inbox(event_type, event_id);
