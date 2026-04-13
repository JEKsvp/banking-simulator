CREATE TABLE accounts
(
    id             UUID           PRIMARY KEY,
    account_number VARCHAR(34)    NOT NULL UNIQUE,
    user_id        UUID           NOT NULL,
    type           VARCHAR(20)    NOT NULL,
    currency          VARCHAR(3)     NOT NULL,
    total_balance     BIGINT         NOT NULL,
    available_balance BIGINT         NOT NULL,
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE INDEX idx_accounts_user_id ON accounts (user_id);

CREATE TABLE transactions
(
    id                     UUID           PRIMARY KEY,
    source_account_id      UUID           NOT NULL REFERENCES accounts (id),
    destination_account_id UUID           NOT NULL REFERENCES accounts (id),
    amount                 BIGINT         NOT NULL,
    currency               VARCHAR(3)     NOT NULL,
    idempotency_key        VARCHAR(255)   NOT NULL UNIQUE,
    description            VARCHAR(500),
    created_at             TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE INDEX idx_transactions_source_account_id ON transactions (source_account_id);
CREATE INDEX idx_transactions_destination_account_id ON transactions (destination_account_id);
