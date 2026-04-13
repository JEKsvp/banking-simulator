package com.abadeksvp.bankingsimulator.domain.command;

import com.abadeksvp.bankingsimulator.cqrs.command.Command;
import com.abadeksvp.bankingsimulator.domain.model.Currency;
import com.abadeksvp.bankingsimulator.domain.model.Money;

import java.util.UUID;

public record CreateUserAccountCommand(
        UUID userId,
        Currency currency,
        boolean overdraftEnabled,
        Money initialDeposit
) implements Command<UUID> {

    public CreateUserAccountCommand {
        if (initialDeposit != null) {
            if (!initialDeposit.isPositive()) {
                throw new IllegalArgumentException("Initial deposit must be positive");
            }
            if (initialDeposit.currency() != currency) {
                throw new IllegalArgumentException("Initial deposit currency must match account currency");
            }
        }
    }
}
