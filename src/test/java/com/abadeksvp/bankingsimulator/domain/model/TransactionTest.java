package com.abadeksvp.bankingsimulator.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionTest {

    @Test
    void shouldTransitionFromPendingToCompleted() {
        Transaction transaction = buildTransaction(TransactionStatus.PENDING);

        transaction.transition(TransactionStatus.COMPLETED);

        assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
    }

    @Test
    void shouldTransitionFromPendingToDeclined() {
        Transaction transaction = buildTransaction(TransactionStatus.PENDING);

        transaction.transition(TransactionStatus.DECLINED);

        assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.DECLINED);
    }

    @Test
    void shouldRejectTransitionFromCompletedToPending() {
        Transaction transaction = buildTransaction(TransactionStatus.COMPLETED);

        assertThatThrownBy(() -> transaction.transition(TransactionStatus.PENDING))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot transition from COMPLETED to PENDING");
    }

    @Test
    void shouldRejectTransitionFromDeclinedToPending() {
        Transaction transaction = buildTransaction(TransactionStatus.DECLINED);

        assertThatThrownBy(() -> transaction.transition(TransactionStatus.PENDING))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot transition from DECLINED to PENDING");
    }

    private Transaction buildTransaction(TransactionStatus status) {
        return Transaction.builder()
                .id(UUID.randomUUID())
                .type(TransactionType.DEPOSIT)
                .status(status)
                .billAccountId(UUID.randomUUID())
                .counterpartAccountId(UUID.randomUUID())
                .amount(new Money(10000, Currency.USD))
                .idempotencyKey(UUID.randomUUID().toString())
                .description("test")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
