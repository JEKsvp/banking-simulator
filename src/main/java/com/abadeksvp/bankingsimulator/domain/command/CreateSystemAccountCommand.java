package com.abadeksvp.bankingsimulator.domain.command;

import com.abadeksvp.bankingsimulator.cqrs.command.Command;
import com.abadeksvp.bankingsimulator.domain.model.Currency;

import java.util.UUID;

public record CreateSystemAccountCommand(
        Currency currency
) implements Command<UUID> {
}
