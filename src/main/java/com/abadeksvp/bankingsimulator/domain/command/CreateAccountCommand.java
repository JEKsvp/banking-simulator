package com.abadeksvp.bankingsimulator.domain.command;

import com.abadeksvp.bankingsimulator.cqrs.command.Command;
import com.abadeksvp.bankingsimulator.domain.model.AccountType;
import com.abadeksvp.bankingsimulator.domain.model.Money;

import java.util.UUID;

public record CreateAccountCommand(
        UUID userId,
        String accountNumber,
        AccountType accountType,
        Money initialDeposit,
        boolean overdraftEnabled
) implements Command<UUID> {
}
