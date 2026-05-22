-- V1__create_stock_prices_table.sql
-- Historique des cours boursiers
-- TimescaleDB hypertable pour optimiser les requêtes sur séries temporelles

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

-- Convertir en hypertable TimescaleDB si l'extension est disponible (optionnel en K8s standard)
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_available_extensions WHERE name = 'timescaledb'
    ) THEN
        PERFORM create_extension_if_not_exists('timescaledb');
        PERFORM create_hypertable('stock_prices', 'fetched_at');
    END IF;
END $$;

-- Index pour les requêtes fréquentes
CREATE INDEX idx_stock_prices_symbol ON stock_prices (symbol, fetched_at DESC);
CREATE INDEX idx_stock_prices_fetched_at ON stock_prices (fetched_at DESC);