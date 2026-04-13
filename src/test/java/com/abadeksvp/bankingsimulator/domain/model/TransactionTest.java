package com.abadeksvp.bankingsimulator.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionTest {

    private static final Instant NOW = Instant.parse("2025-01-15T10:00:00Z");
    private static final Instant LATER = Instant.parse("2025-01-15T11:00:00Z");

    @Test
    void shouldTransitionFromPendingToCompleted() {
        Transaction transaction = buildTransaction(TransactionStatus.PENDING);

        transaction.transition(TransactionStatus.COMPLETED, LATER);

        assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(transaction.getUpdatedAt()).isEqualTo(LATER);
    }

    @Test
    void shouldTransitionFromPendingToDeclined() {
        Transaction transaction = buildTransaction(TransactionStatus.PENDING);

        transaction.transition(TransactionStatus.DECLINED, LATER);

        assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.DECLINED);
        assertThat(transaction.getUpdatedAt()).isEqualTo(LATER);
    }

    @Test
    void shouldRejectTransitionFromCompletedToPending() {
        Transaction transaction = buildTransaction(TransactionStatus.COMPLETED);

        assertThatThrownBy(() -> transaction.transition(TransactionStatus.PENDING, LATER))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot transition from COMPLETED to PENDING");
    }

    @Test
    void shouldRejectTransitionFromDeclinedToPending() {
        Transaction transaction = buildTransaction(TransactionStatus.DECLINED);

        assertThatThrownBy(() -> transaction.transition(TransactionStatus.PENDING, LATER))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot transition from DECLINED to PENDING");
    }

    @Test
    void shouldRejectTransitionFromCompletedToDeclined() {
        Transaction transaction = buildTransaction(TransactionStatus.COMPLETED);

        assertThatThrownBy(() -> transaction.transition(TransactionStatus.DECLINED, LATER))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot transition from COMPLETED to DECLINED");
    }

    @Test
    void shouldRejectTransitionFromDeclinedToCompleted() {
        Transaction transaction = buildTransaction(TransactionStatus.DECLINED);

        assertThatThrownBy(() -> transaction.transition(TransactionStatus.COMPLETED, LATER))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot transition from DECLINED to COMPLETED");
    }

    @Test
    void shouldDeclineWithReason() {
        Transaction transaction = buildTransaction(TransactionStatus.PENDING);

        transaction.decline("Insufficient funds", LATER);

        assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.DECLINED);
        assertThat(transaction.getDeclineReason()).isEqualTo("Insufficient funds");
        assertThat(transaction.getUpdatedAt()).isEqualTo(LATER);
    }

    private Transaction buildTransaction(TransactionStatus status) {
        return Transaction.builder()
                .id(UUID.randomUUID())
                .status(status)
                .sourceAccountId(UUID.randomUUID())
                .destinationAccountId(UUID.randomUUID())
                .amount(new Money(10000, Currency.USD))
                .idempotencyKey(UUID.randomUUID().toString())
                .description("test")
                .createdAt(NOW)
                .updatedAt(NOW)
                .build();
    }
}
