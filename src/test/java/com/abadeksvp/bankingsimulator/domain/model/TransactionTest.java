package com.abadeksvp.bankingsimulator.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionTest {

    @Test
    void shouldTransitionFromCreatedToPending() {
        Transaction transaction = buildTransaction(TransactionStatus.CREATED);

        transaction.transition(TransactionStatus.PENDING);

        assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.PENDING);
    }

    @Test
    void shouldTransitionFromCreatedToDeclined() {
        Transaction transaction = buildTransaction(TransactionStatus.CREATED);

        transaction.transition(TransactionStatus.DECLINED);

        assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.DECLINED);
    }

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

    @Test
    void shouldRejectTransitionFromCreatedToCompleted() {
        Transaction transaction = buildTransaction(TransactionStatus.CREATED);

        assertThatThrownBy(() -> transaction.transition(TransactionStatus.COMPLETED))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot transition from CREATED to COMPLETED");
    }

    private Transaction buildTransaction(TransactionStatus status) {
        return Transaction.builder()
                .id(UUID.randomUUID())
                .type(TransactionType.DEPOSIT)
                .status(status)
                .billAccountId(UUID.randomUUID())
                .counterpartAccountId(UUID.randomUUID())
                .billingAmount(new Money(new BigDecimal("100.00"), Currency.USD))
                .transactionAmount(new Money(new BigDecimal("100.00"), Currency.USD))
                .description("test")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
