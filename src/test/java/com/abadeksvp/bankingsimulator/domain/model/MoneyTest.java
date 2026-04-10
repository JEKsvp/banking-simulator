package com.abadeksvp.bankingsimulator.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @Test
    void shouldAddMoneyWithSameCurrency() {
        Money a = new Money(new BigDecimal("10.00"), Currency.USD);
        Money b = new Money(new BigDecimal("5.50"), Currency.USD);

        assertThat(a.add(b)).isEqualTo(new Money(new BigDecimal("15.50"), Currency.USD));
    }

    @Test
    void shouldSubtractMoneyWithSameCurrency() {
        Money a = new Money(new BigDecimal("10.00"), Currency.USD);
        Money b = new Money(new BigDecimal("3.50"), Currency.USD);

        assertThat(a.subtract(b)).isEqualTo(new Money(new BigDecimal("6.50"), Currency.USD));
    }

    @Test
    void shouldThrowOnAddWithDifferentCurrencies() {
        Money usd = new Money(new BigDecimal("10.00"), Currency.USD);
        Money eur = new Money(new BigDecimal("5.00"), Currency.EUR);

        assertThatThrownBy(() -> usd.add(eur))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Currency mismatch");
    }

    @Test
    void shouldThrowOnSubtractWithDifferentCurrencies() {
        Money usd = new Money(new BigDecimal("10.00"), Currency.USD);
        Money eur = new Money(new BigDecimal("5.00"), Currency.EUR);

        assertThatThrownBy(() -> usd.subtract(eur))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Currency mismatch");
    }

    @Test
    void shouldThrowOnNullAmount() {
        assertThatThrownBy(() -> new Money(null, Currency.USD))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Amount must not be null");
    }

    @Test
    void shouldThrowOnNullCurrency() {
        assertThatThrownBy(() -> new Money(new BigDecimal("10.00"), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Currency code must not be null");
    }

    @Test
    void shouldBeEqualRegardlessOfScale() {
        Money a = new Money(new BigDecimal("10.0"), Currency.USD);
        Money b = new Money(new BigDecimal("10.00"), Currency.USD);

        assertThat(a).isEqualTo(b);
    }

    @Test
    void shouldBePositive() {
        Money money = new Money(new BigDecimal("10.00"), Currency.USD);

        assertThat(money.isPositive()).isTrue();
        assertThat(money.isNegative()).isFalse();
        assertThat(money.isZero()).isFalse();
    }

    @Test
    void shouldBeNegative() {
        Money money = new Money(new BigDecimal("-5.00"), Currency.USD);

        assertThat(money.isPositive()).isFalse();
        assertThat(money.isNegative()).isTrue();
        assertThat(money.isZero()).isFalse();
    }

    @Test
    void shouldBeZero() {
        Money money = new Money(BigDecimal.ZERO, Currency.USD);

        assertThat(money.isPositive()).isFalse();
        assertThat(money.isNegative()).isFalse();
        assertThat(money.isZero()).isTrue();
    }
}
