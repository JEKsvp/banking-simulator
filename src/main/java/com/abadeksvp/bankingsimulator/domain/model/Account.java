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
import org.springframework.data.relational.core.mapping.Embedded;
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

    @Embedded(onEmpty = Embedded.OnEmpty.USE_NULL)
    private Money balance;

    @Column("created_at")
    private Instant createdAt;

    @Column("updated_at")
    private Instant updatedAt;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @PersistenceCreator
    public Account(UUID id, String accountNumber, UUID userId, AccountType type,
                   Money balance, Instant createdAt, Instant updatedAt) {
        this(id, accountNumber, userId, type, balance, createdAt, updatedAt, false);
    }

    @Override
    public boolean isNew() {
        return isNew;
    }
}
