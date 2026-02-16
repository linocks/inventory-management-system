CREATE SEQUENCE products_id_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE outbox_events_id_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE products (
    id              BIGINT PRIMARY KEY DEFAULT nextval('products_id_seq'),
    sku             VARCHAR(50) UNIQUE NOT NULL,
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    category        VARCHAR(100),
    price           DECIMAL(10,2) NOT NULL,
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

CREATE INDEX idx_outbox_unprocessed ON outbox_events(processed, created_at) WHERE processed = FALSE;
CREATE INDEX idx_outbox_status_next_attempt ON outbox_events(status, next_attempt_at, created_at);
