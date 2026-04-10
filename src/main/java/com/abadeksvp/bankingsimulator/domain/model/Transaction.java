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

@Table("transactions")
@Getter
@Builder
@AllArgsConstructor
@EqualsAndHashCode(exclude = "isNew")
public class Transaction implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column("type")
    private TransactionType type;

    @Column("source_account_id")
    private UUID sourceAccountId;

    @Column("target_account_id")
    private UUID targetAccountId;

    @Embedded(onEmpty = Embedded.OnEmpty.USE_NULL)
    private Money transactionAmount;

    @Column("description")
    private String description;

    @Column("created_at")
    private Instant createdAt;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @PersistenceCreator
    public Transaction(UUID id, TransactionType type, UUID sourceAccountId, UUID targetAccountId,
                       Money transactionAmount, String description, Instant createdAt) {
        this(id, type, sourceAccountId, targetAccountId, transactionAmount, description, createdAt, false);
    }

    @Override
    public boolean isNew() {
        return isNew;
    }
}
