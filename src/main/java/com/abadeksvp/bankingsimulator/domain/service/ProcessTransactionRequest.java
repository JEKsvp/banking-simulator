package com.abadeksvp.bankingsimulator.domain.service;

import com.abadeksvp.bankingsimulator.domain.model.Money;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

public record ProcessTransactionRequest(
        String idempotencyKey,
        UUID sourceAccountId,
        UUID destinationAccountId,
        Money amount,
        String description
) {
    public ProcessTransactionRequest {
        requireNonNull(sourceAccountId, "Source account ID must not be null");
        requireNonNull(destinationAccountId, "Destination account ID must not be null");
        requireNonNull(amount, "Amount must not be null");
        requireNonNull(idempotencyKey, "Idempotency key must not be null");
        if (idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency key must not be blank");
        }
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (sourceAccountId.equals(destinationAccountId)) {
            throw new IllegalArgumentException("Source and destination accounts cannot be the same");
        }
    }
}
