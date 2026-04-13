package com.abadeksvp.bankingsimulator.domain.service;

import com.abadeksvp.bankingsimulator.domain.model.Currency;
import com.abadeksvp.bankingsimulator.domain.model.Money;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProcessTransactionRequestTest {

    private static final UUID SOURCE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID DESTINATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final Money VALID_AMOUNT = new Money(10000, Currency.USD);

    @Test
    void shouldRejectNullIdempotencyKey() {
        assertThatThrownBy(() -> new ProcessTransactionRequest(
                null, SOURCE_ID, DESTINATION_ID, VALID_AMOUNT, "desc"))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Idempotency key must not be null");
    }

    @Test
    void shouldRejectBlankIdempotencyKey() {
        assertThatThrownBy(() -> new ProcessTransactionRequest(
                "  ", SOURCE_ID, DESTINATION_ID, VALID_AMOUNT, "desc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Idempotency key must not be blank");
    }

    @Test
    void shouldRejectNullSourceAccountId() {
        assertThatThrownBy(() -> new ProcessTransactionRequest(
                "key-001", null, DESTINATION_ID, VALID_AMOUNT, "desc"))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Source account ID must not be null");
    }

    @Test
    void shouldRejectNullDestinationAccountId() {
        assertThatThrownBy(() -> new ProcessTransactionRequest(
                "key-001", SOURCE_ID, null, VALID_AMOUNT, "desc"))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Destination account ID must not be null");
    }

    @Test
    void shouldRejectNullAmount() {
        assertThatThrownBy(() -> new ProcessTransactionRequest(
                "key-001", SOURCE_ID, DESTINATION_ID, null, "desc"))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Amount must not be null");
    }

    @Test
    void shouldRejectZeroAmount() {
        assertThatThrownBy(() -> new ProcessTransactionRequest(
                "key-001", SOURCE_ID, DESTINATION_ID, new Money(0, Currency.USD), "desc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Amount must be positive");
    }

    @Test
    void shouldRejectNegativeAmount() {
        assertThatThrownBy(() -> new ProcessTransactionRequest(
                "key-001", SOURCE_ID, DESTINATION_ID, new Money(-100, Currency.USD), "desc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Amount must be positive");
    }

    @Test
    void shouldRejectSameSourceAndDestination() {
        assertThatThrownBy(() -> new ProcessTransactionRequest(
                "key-001", SOURCE_ID, SOURCE_ID, VALID_AMOUNT, "desc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Source and destination accounts cannot be the same");
    }

    @Test
    void shouldAllowNullDescription() {
        new ProcessTransactionRequest("key-001", SOURCE_ID, DESTINATION_ID, VALID_AMOUNT, null);
    }
}
