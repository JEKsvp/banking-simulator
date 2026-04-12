package com.abadeksvp.bankingsimulator.domain.model;

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

    @Column("total_balance")
    private long totalBalance;

    @Column("available_balance")
    private long availableBalance;

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

    @Override
    public boolean isNew() {
        return isNew;
    }
}
