CREATE SEQUENCE stock_id_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE outbox_events_id_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE processed_events_id_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE stock (
    id              BIGINT PRIMARY KEY DEFAULT nextval('stock_id_seq'),
    product_id      BIGINT NOT NULL,
    sku             VARCHAR(50) UNIQUE NOT NULL,
    quantity         INT NOT NULL DEFAULT 0,
    min_threshold   INT DEFAULT 10,
    version         BIGINT NOT NULL DEFAULT 0,
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP,
    created_by      VARCHAR(100),
    modified_by     VARCHAR(100)
);

CREATE TABLE outbox_events (
    id              BIGINT PRIMARY KEY DEFAULT nextval('outbox_events_id_seq'),
    event_id        VARCHAR(255) UNIQUE NOT NULL,
    event_type      VARCHAR(100) NOT NULL,
    topic           VARCHAR(255) NOT NULL,
    event_key       VARCHAR(255),
    payload         TEXT NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed       BOOLEAN DEFAULT FALSE,
    status          VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    retry_count     INT NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP,
    claimed_at      TIMESTAMP,
    last_error      TEXT,
    processed_at    TIMESTAMP
);

CREATE TABLE processed_events (
    id              BIGINT PRIMARY KEY DEFAULT nextval('processed_events_id_seq'),
    event_id        VARCHAR(255) UNIQUE NOT NULL,
    topic           VARCHAR(255) NOT NULL,
    processed_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE inventory_summary_projection (
    id                  BIGINT PRIMARY KEY,
    total_products      BIGINT NOT NULL DEFAULT 0,
    total_stock_units   BIGINT NOT NULL DEFAULT 0,
    low_stock_products  BIGINT NOT NULL DEFAULT 0,
    out_of_stock_products BIGINT NOT NULL DEFAULT 0,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_outbox_unprocessed ON outbox_events(processed, created_at) WHERE processed = FALSE;
CREATE INDEX idx_outbox_status_next_attempt ON outbox_events(status, next_attempt_at, created_at);
CREATE INDEX idx_processed_events_event_id ON processed_events(event_id);
CREATE INDEX idx_stock_sku ON stock(sku);
CREATE INDEX idx_stock_product_id ON stock(product_id);
CREATE INDEX idx_stock_low_stock ON stock(quantity, min_threshold);
CREATE INDEX idx_stock_out_of_stock ON stock(quantity) WHERE quantity = 0;
