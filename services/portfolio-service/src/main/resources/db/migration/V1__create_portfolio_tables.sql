CREATE TABLE portfolios (
    id          BIGSERIAL       PRIMARY KEY,
    user_id     VARCHAR(50)     NOT NULL,
    name        VARCHAR(100)    NOT NULL,
    description VARCHAR(255),
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE TABLE positions (
    id              BIGSERIAL       PRIMARY KEY,
    portfolio_id    BIGINT          NOT NULL REFERENCES portfolios(id),
    symbol          VARCHAR(10)     NOT NULL,
    quantity        DECIMAL(15,4)   NOT NULL,
    avg_buy_price   DECIMAL(15,4)   NOT NULL,
    current_price   DECIMAL(15,4),
    last_updated    TIMESTAMPTZ,
    CONSTRAINT uq_portfolio_symbol UNIQUE (portfolio_id, symbol)
);

CREATE INDEX idx_positions_portfolio ON positions(portfolio_id);
CREATE INDEX idx_positions_symbol ON positions(symbol);

CREATE TABLE transactions (
    id              BIGSERIAL       PRIMARY KEY,
    portfolio_id    BIGINT          NOT NULL REFERENCES portfolios(id),
    symbol          VARCHAR(10)     NOT NULL,
    type            VARCHAR(10)     NOT NULL CHECK (type IN ('BUY', 'SELL')),
    quantity        DECIMAL(15,4)   NOT NULL,
    price           DECIMAL(15,4)   NOT NULL,
    total_amount    DECIMAL(15,4)   NOT NULL,
    executed_at     TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_transactions_portfolio ON transactions(portfolio_id);
CREATE INDEX idx_transactions_symbol ON transactions(symbol);