-- V1__create_stock_prices_table.sql
-- Historique des cours boursiers

CREATE TABLE stock_prices (
    id          BIGSERIAL       NOT NULL,
    symbol      VARCHAR(10)     NOT NULL,
    price       DECIMAL(15, 4)  NOT NULL,
    volume      BIGINT,
    change_pct  DECIMAL(8, 4),
    fetched_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    source      VARCHAR(50)     NOT NULL DEFAULT 'ALPHA_VANTAGE',
    CONSTRAINT pk_stock_prices PRIMARY KEY (id, fetched_at)
);

-- Index pour les requêtes fréquentes
CREATE INDEX idx_stock_prices_symbol ON stock_prices (symbol, fetched_at DESC);
CREATE INDEX idx_stock_prices_fetched_at ON stock_prices (fetched_at DESC);