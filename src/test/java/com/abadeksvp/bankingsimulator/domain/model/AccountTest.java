package com.abadeksvp.bankingsimulator.domain.model;

import com.abadeksvp.bankingsimulator.domain.error.CurrencyMismatchException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountTest {

    private static final Instant NOW = Instant.parse("2025-01-15T10:00:00Z");
    private static final Instant LATER = Instant.parse("2025-01-15T11:00:00Z");

    @Test
    void shouldReduceAvailableBalanceOnHold() {
        Account account = buildAccount(false);
        account.credit(new Money(100000, Currency.USD), NOW);

        account.hold(new Money(30000, Currency.USD), LATER);

        assertThat(account.getAvailableBalance()).isEqualTo(new Money(70000, Currency.USD));
        assertThat(account.getTotalBalance()).isEqualTo(new Money(100000, Currency.USD));
        assertThat(account.getUpdatedAt()).isEqualTo(LATER);
    }

    @Test
    void shouldReduceTotalBalanceOnDebit() {
        Account account = buildAccount(false);
        account.credit(new Money(100000, Currency.USD), NOW);
        account.hold(new Money(30000, Currency.USD), NOW);

        account.debit(new Money(30000, Currency.USD), LATER);

        assertThat(account.getTotalBalance()).isEqualTo(new Money(70000, Currency.USD));
        assertThat(account.getAvailableBalance()).isEqualTo(new Money(70000, Currency.USD));
        assertThat(account.getUpdatedAt()).isEqualTo(LATER);
    }

    @Test
    void shouldIncreaseBothBalancesOnCredit() {
        Account account = buildAccount(false);
        account.credit(new Money(100000, Currency.USD), NOW);

        account.credit(new Money(50000, Currency.USD), LATER);

        assertThat(account.getTotalBalance()).isEqualTo(new Money(150000, Currency.USD));
        assertThat(account.getAvailableBalance()).isEqualTo(new Money(150000, Currency.USD));
        assertThat(account.getUpdatedAt()).isEqualTo(LATER);
    }

    @Test
    void shouldRestoreAvailableBalanceOnReleaseHold() {
        Account account = buildAccount(false);
        account.credit(new Money(100000, Currency.USD), NOW);
        account.hold(new Money(30000, Currency.USD), NOW);

        account.releaseHold(new Money(30000, Currency.USD), LATER);

        assertThat(account.getAvailableBalance()).isEqualTo(new Money(100000, Currency.USD));
        assertThat(account.getTotalBalance()).isEqualTo(new Money(100000, Currency.USD));
        assertThat(account.getUpdatedAt()).isEqualTo(LATER);
    }

    @Test
    void shouldReturnTrueWhenSufficientBalance() {
        Account account = buildAccount(false);
        account.credit(new Money(100000, Currency.USD), NOW);

        assertThat(account.hasSufficientBalance(new Money(100000, Currency.USD))).isTrue();
        assertThat(account.hasSufficientBalance(new Money(50000, Currency.USD))).isTrue();
    }

    @Test
    void shouldReturnFalseWhenInsufficientBalance() {
        Account account = buildAccount(false);
        account.credit(new Money(100000, Currency.USD), NOW);

        assertThat(account.hasSufficientBalance(new Money(100001, Currency.USD))).isFalse();
    }

    @Test
    void shouldAllowOverdraftWhenEnabled() {
        Account account = buildAccount(true);

        assertThat(account.hasSufficientBalance(new Money(100000, Currency.USD))).isTrue();
    }

    @Test
    void shouldThrowOnCurrencyMismatch() {
        Account account = buildAccount(false);
        Money eurAmount = new Money(10000, Currency.EUR);

        assertThatThrownBy(() -> account.hold(eurAmount, LATER))
                .isInstanceOf(CurrencyMismatchException.class)
                .hasMessage("Account currency USD does not match transaction currency EUR");

        assertThatThrownBy(() -> account.debit(eurAmount, LATER))
                .isInstanceOf(CurrencyMismatchException.class);

        assertThatThrownBy(() -> account.credit(eurAmount, LATER))
                .isInstanceOf(CurrencyMismatchException.class);

        assertThatThrownBy(() -> account.releaseHold(eurAmount, LATER))
                .isInstanceOf(CurrencyMismatchException.class);

        assertThatThrownBy(() -> account.hasSufficientBalance(eurAmount))
                .isInstanceOf(CurrencyMismatchException.class);
    }

    private Account buildAccount(boolean overdraftEnabled) {
        return Account.builder()
                .id(UUID.randomUUID())
                .accountNumber("ACC-001")
                .userId(UUID.randomUUID())
                .type(AccountType.USER)
                .currency(Currency.USD)
                .overdraftEnabled(overdraftEnabled)
                .createdAt(NOW)
                .updatedAt(NOW)
                .build();
    }
}
