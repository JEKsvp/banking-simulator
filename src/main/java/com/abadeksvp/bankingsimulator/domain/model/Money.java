package com.abadeksvp.bankingsimulator.domain.model;

import org.springframework.data.relational.core.mapping.Column;

public record Money(
        @Column("amount") long amount,
        @Column("currency") Currency currency
) {

    public Money {
        if (currency == null) {
            throw new IllegalArgumentException("Currency must not be null");
        }
    }

    public Money add(Money other) {
        validateSameCurrency(other);
        return new Money(this.amount + other.amount, this.currency);
    }

    public Money subtract(Money other) {
        validateSameCurrency(other);
        return new Money(this.amount - other.amount, this.currency);
    }

    public boolean isPositive() {
        return amount > 0;
    }

    public boolean isNegative() {
        return amount < 0;
    }

    public boolean isZero() {
        return amount == 0;
    }

    private void validateSameCurrency(Money other) {
        if (this.currency != other.currency) {
            throw new IllegalArgumentException(
                    "Currency mismatch: %s vs %s".formatted(this.currency, other.currency)
            );
        }
    }
}
