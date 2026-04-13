package com.abadeksvp.bankingsimulator.domain.error;

import java.util.UUID;

public class AccountNotFoundException extends RuntimeException {

    private final UUID accountId;

    public AccountNotFoundException(UUID accountId) {
        super("Account with id %s not found".formatted(accountId));
        this.accountId = accountId;
    }

    public UUID getAccountId() {
        return accountId;
    }
}
