package com.abadeksvp.bankingsimulator.domain.model;

import com.abadeksvp.bankingsimulator.domain.error.CurrencyMismatchException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table("accounts")
@Getter
@Builder
@AllArgsConstructor
@EqualsAndHashCode(exclude = "isNew")
public class Account implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column("account_number")
    private String accountNumber;

    @Column("user_id")
    private UUID userId;

    @Column("type")
    private AccountType type;

    @Column("currency")
    private Currency currency;

    @Getter(AccessLevel.NONE)
    @Column("total_balance")
    @Builder.Default
    private long totalBalance = 0;

    @Getter(AccessLevel.NONE)
    @Column("available_balance")
    @Builder.Default
    private long availableBalance = 0;

    @Column("created_at")
    private Instant createdAt;

    @Column("updated_at")
    private Instant updatedAt;

    @Column("overdraft_enabled")
    private boolean overdraftEnabled;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @PersistenceCreator
    public Account(UUID id, String accountNumber, UUID userId, AccountType type,
                   Currency currency, long totalBalance, long availableBalance,
                   Instant createdAt, Instant updatedAt, boolean overdraftEnabled) {
        this(id, accountNumber, userId, type, currency, totalBalance, availableBalance,
                createdAt, updatedAt, overdraftEnabled, false);
    }

    public Money getTotalBalance() {
        return new Money(totalBalance, currency);
    }

    public Money getAvailableBalance() {
        return new Money(availableBalance, currency);
    }

    public boolean hasSufficientBalance(Money amount) {
        validateCurrency(amount);
        return overdraftEnabled || availableBalance >= amount.amount();
    }

    public void hold(Money amount, Instant now) {
        validateCurrency(amount);
        this.availableBalance -= amount.amount();
        this.updatedAt = now;
    }

    public void debit(Money amount, Instant now) {
        validateCurrency(amount);
        this.totalBalance -= amount.amount();
        this.updatedAt = now;
    }

    public void credit(Money amount, Instant now) {
        validateCurrency(amount);
        this.totalBalance += amount.amount();
        this.availableBalance += amount.amount();
        this.updatedAt = now;
    }

    public void releaseHold(Money amount, Instant now) {
        validateCurrency(amount);
        this.availableBalance += amount.amount();
        this.updatedAt = now;
    }

    private void validateCurrency(Money amount) {
        if (amount.currency() != this.currency) {
            throw new CurrencyMismatchException(this.currency, amount.currency());
        }
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

}
