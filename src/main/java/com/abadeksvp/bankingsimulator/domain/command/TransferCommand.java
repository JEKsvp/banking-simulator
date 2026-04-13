package com.abadeksvp.bankingsimulator.domain.command;

import com.abadeksvp.bankingsimulator.cqrs.command.Command;
import com.abadeksvp.bankingsimulator.domain.model.Money;

import java.util.UUID;

import static java.util.Objects.requireNonNull;

public record TransferCommand(
        String idempotencyKey,
        UUID sourceAccountId,
        UUID destinationAccountId,
        Money amount,
        String description
) implements Command<UUID> {

    public TransferCommand {
        requireNonNull(idempotencyKey, "Idempotency key must not be null");
        if (idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency key must not be blank");
        }
        requireNonNull(sourceAccountId, "Source account ID must not be null");
        requireNonNull(destinationAccountId, "Destination account ID must not be null");
        requireNonNull(amount, "Amount must not be null");
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (sourceAccountId.equals(destinationAccountId)) {
            throw new IllegalArgumentException("Source and destination accounts cannot be the same");
        }
    }
}
