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

    @Column("status")
    private TransactionStatus status;

    @Column("bill_account_id")
    private UUID billAccountId;

    @Column("counterpart_account_id")
    private UUID counterpartAccountId;

    @Embedded(onEmpty = Embedded.OnEmpty.USE_NULL, prefix = "billing_")
    private Money billingAmount;

    @Embedded(onEmpty = Embedded.OnEmpty.USE_NULL, prefix = "transaction_")
    private Money transactionAmount;

    @Column("description")
    private String description;

    @Column("created_at")
    private Instant createdAt;

    @Column("updated_at")
    private Instant updatedAt;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @PersistenceCreator
    public Transaction(UUID id, TransactionType type, TransactionStatus status,
                       UUID billAccountId, UUID counterpartAccountId,
                       Money billingAmount, Money transactionAmount, String description,
                       Instant createdAt, Instant updatedAt) {
        this(id, type, status, billAccountId, counterpartAccountId,
                billingAmount, transactionAmount, description, createdAt, updatedAt, false);
    }

    public void transition(TransactionStatus newStatus) {
        if (!status.canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                    "Cannot transition from %s to %s".formatted(status, newStatus));
        }
        this.status = newStatus;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }
}
