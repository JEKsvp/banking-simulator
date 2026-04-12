package com.abadeksvp.bankingsimulator.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @Test
    void shouldAddMoneyWithSameCurrency() {
        Money a = new Money(1000, Currency.USD);
        Money b = new Money(550, Currency.USD);

        assertThat(a.add(b)).isEqualTo(new Money(1550, Currency.USD));
    }

    @Test
    void shouldSubtractMoneyWithSameCurrency() {
        Money a = new Money(1000, Currency.USD);
        Money b = new Money(350, Currency.USD);

        assertThat(a.subtract(b)).isEqualTo(new Money(650, Currency.USD));
    }

    @Test
    void shouldThrowOnAddWithDifferentCurrencies() {
        Money usd = new Money(1000, Currency.USD);
        Money eur = new Money(500, Currency.EUR);

        assertThatThrownBy(() -> usd.add(eur))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Currency mismatch");
    }

    @Test
    void shouldThrowOnSubtractWithDifferentCurrencies() {
        Money usd = new Money(1000, Currency.USD);
        Money eur = new Money(500, Currency.EUR);

        assertThatThrownBy(() -> usd.subtract(eur))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Currency mismatch");
    }

    @Test
    void shouldThrowOnNullCurrency() {
        assertThatThrownBy(() -> new Money(1000, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Currency must not be null");
    }

    @Test
    void shouldBePositive() {
        Money money = new Money(1000, Currency.USD);

        assertThat(money.isPositive()).isTrue();
        assertThat(money.isNegative()).isFalse();
        assertThat(money.isZero()).isFalse();
    }

    @Test
    void shouldBeNegative() {
        Money money = new Money(-500, Currency.USD);

        assertThat(money.isPositive()).isFalse();
        assertThat(money.isNegative()).isTrue();
        assertThat(money.isZero()).isFalse();
    }

    @Test
    void shouldBeZero() {
        Money money = new Money(0, Currency.USD);

        assertThat(money.isPositive()).isFalse();
        assertThat(money.isNegative()).isFalse();
        assertThat(money.isZero()).isTrue();
    }
}
