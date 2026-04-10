CREATE TABLE accounts
(
    id             UUID           PRIMARY KEY,
    account_number VARCHAR(34)    NOT NULL UNIQUE,
    user_id        UUID           NOT NULL,
    type           VARCHAR(20)    NOT NULL,
    amount         NUMERIC(19, 4) NOT NULL,
    currency  VARCHAR(3)     NOT NULL,
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE INDEX idx_accounts_user_id ON accounts (user_id);

CREATE TABLE transactions
(
    id                UUID           PRIMARY KEY,
    type              VARCHAR(20)    NOT NULL,
    bill_account_id UUID           NOT NULL REFERENCES accounts (id),
    counterpart_account_id UUID           NOT NULL REFERENCES accounts (id),
    billing_amount            NUMERIC(19, 4) NOT NULL,
    billing_currency     VARCHAR(3)     NOT NULL,
    transaction_amount        NUMERIC(19, 4) NOT NULL,
    transaction_currency VARCHAR(3)     NOT NULL,
    description               VARCHAR(500),
    created_at        TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE INDEX idx_transactions_bill_account_id ON transactions (bill_account_id);
CREATE INDEX idx_transactions_counterpart_account_id ON transactions (counterpart_account_id);
