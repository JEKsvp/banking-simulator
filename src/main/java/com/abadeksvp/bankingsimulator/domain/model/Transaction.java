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

    @Column("status")
    private TransactionStatus status;

    @Column("source_account_id")
    private UUID sourceAccountId;

    @Column("destination_account_id")
    private UUID destinationAccountId;

    @Embedded(onEmpty = Embedded.OnEmpty.USE_NULL)
    private Money amount;

    @Column("idempotency_key")
    private String idempotencyKey;

    @Column("description")
    private String description;

    @Column("decline_reason")
    private String declineReason;

    @Column("created_at")
    private Instant createdAt;

    @Column("updated_at")
    private Instant updatedAt;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @PersistenceCreator
    public Transaction(UUID id, TransactionStatus status,
                       UUID sourceAccountId, UUID destinationAccountId,
                       Money amount, String idempotencyKey, String description,
                       String declineReason,
                       Instant createdAt, Instant updatedAt) {
        this(id, status, sourceAccountId, destinationAccountId,
                amount, idempotencyKey, description, declineReason, createdAt, updatedAt, false);
    }

    public void transition(TransactionStatus newStatus, Instant now) {
        if (!status.canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                    "Cannot transition from %s to %s".formatted(status, newStatus));
        }
        this.status = newStatus;
        this.updatedAt = now;
    }

    public void decline(String reason, Instant now) {
        transition(TransactionStatus.DECLINED, now);
        this.declineReason = reason;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }
}
