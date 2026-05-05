CREATE TABLE dlt_events (
    id            BIGSERIAL    PRIMARY KEY,
    topic         VARCHAR(100) NOT NULL,
    symbol        VARCHAR(10)  NOT NULL,
    price         DECIMAL(15,4),
    error_message TEXT,
    failed_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
