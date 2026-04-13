package com.abadeksvp.bankingsimulator.domain.query;

import com.abadeksvp.bankingsimulator.domain.model.Account;
import com.abadeksvp.bankingsimulator.domain.model.AccountType;
import com.abadeksvp.bankingsimulator.domain.model.Currency;
import com.abadeksvp.bankingsimulator.domain.model.Money;

import java.time.Instant;
import java.util.UUID;

public record AccountView(
        UUID id,
        String accountNumber,
        UUID userId,
        AccountType type,
        Currency currency,
        Money totalBalance,
        Money availableBalance,
        boolean overdraftEnabled,
        Instant createdAt,
        Instant updatedAt
) {

    public static AccountView from(Account account) {
        return new AccountView(
                account.getId(),
                account.getAccountNumber(),
                account.getUserId(),
                account.getType(),
                account.getCurrency(),
                account.getTotalBalance(),
                account.getAvailableBalance(),
                account.isOverdraftEnabled(),
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
    }
}
