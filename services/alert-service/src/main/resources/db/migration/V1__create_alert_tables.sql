CREATE TABLE alert_rules (
    id              BIGSERIAL       PRIMARY KEY,
    user_id         VARCHAR(50)     NOT NULL,
    symbol          VARCHAR(10)     NOT NULL,
    threshold_price DECIMAL(15,4)   NOT NULL,
    alert_type      VARCHAR(10)     NOT NULL CHECK (alert_type IN ('ABOVE', 'BELOW')),
    email           VARCHAR(100)    NOT NULL,
    active          BOOLEAN         NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_alert_rules_symbol ON alert_rules(symbol, active);
CREATE INDEX idx_alert_rules_user ON alert_rules(user_id);

CREATE TABLE alert_history (
    id              BIGSERIAL       PRIMARY KEY,
    alert_rule_id   BIGINT          NOT NULL REFERENCES alert_rules(id),
    symbol          VARCHAR(10)     NOT NULL,
    triggered_price DECIMAL(15,4)   NOT NULL,
    threshold_price DECIMAL(15,4)   NOT NULL,
    alert_type      VARCHAR(10)     NOT NULL,
    email_sent      BOOLEAN         NOT NULL DEFAULT false,
    triggered_at    TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_alert_history_rule ON alert_history(alert_rule_id);
CREATE INDEX idx_alert_history_symbol ON alert_history(symbol);