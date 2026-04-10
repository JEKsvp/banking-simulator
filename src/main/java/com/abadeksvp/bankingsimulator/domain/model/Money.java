package com.abadeksvp.bankingsimulator.domain.model;

import org.springframework.data.relational.core.mapping.Column;

import java.math.BigDecimal;

public record Money(
        @Column("amount") BigDecimal amount,
        @Column("currency") Currency currency
) {

    public Money {
        if (amount == null) {
            throw new IllegalArgumentException("Amount must not be null");
        }
        if (currency == null) {
            throw new IllegalArgumentException("Currency code must not be null");
        }
    }

    public Money add(Money other) {
        validateSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money subtract(Money other) {
        validateSameCurrency(other);
        return new Money(this.amount.subtract(other.amount), this.currency);
    }

    public boolean isPositive() {
        return amount.signum() > 0;
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Money other)) return false;
        return this.amount.compareTo(other.amount) == 0 && this.currency == other.currency;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(amount.stripTrailingZeros().toPlainString(), currency);
    }

    private void validateSameCurrency(Money other) {
        if (this.currency != other.currency) {
            throw new IllegalArgumentException(
                    "Currency mismatch: %s vs %s".formatted(this.currency, other.currency)
            );
        }
    }
}
