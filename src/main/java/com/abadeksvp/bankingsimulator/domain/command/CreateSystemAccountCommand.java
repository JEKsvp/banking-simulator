package com.abadeksvp.bankingsimulator.domain.command;

import com.abadeksvp.bankingsimulator.cqrs.command.Command;
import com.abadeksvp.bankingsimulator.domain.model.Currency;

import java.util.UUID;

import static java.util.Objects.requireNonNull;

public record CreateSystemAccountCommand(
        Currency currency
) implements Command<UUID> {

    public CreateSystemAccountCommand {
        requireNonNull(currency, "Currency must not be null");
    }
}
