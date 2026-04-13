package com.abadeksvp.bankingsimulator.domain.error;

import java.util.UUID;

public class InsufficientFundsException extends RuntimeException {

    private final UUID accountId;

    public InsufficientFundsException(UUID accountId) {
        super("Insufficient funds in account %s".formatted(accountId));
        this.accountId = accountId;
    }

    public UUID getAccountId() {
        return accountId;
    }
}
