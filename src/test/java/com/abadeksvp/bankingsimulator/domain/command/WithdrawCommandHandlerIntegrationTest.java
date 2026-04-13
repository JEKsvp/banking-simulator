package com.abadeksvp.bankingsimulator.domain.command;

import com.abadeksvp.bankingsimulator.BaseIntegrationTest;
import com.abadeksvp.bankingsimulator.cqrs.command.CommandBus;
import com.abadeksvp.bankingsimulator.cqrs.core.AppError;
import com.abadeksvp.bankingsimulator.cqrs.core.Result;
import com.abadeksvp.bankingsimulator.domain.error.AccountErrorCode;
import com.abadeksvp.bankingsimulator.domain.error.TransactionErrorCode;
import com.abadeksvp.bankingsimulator.domain.model.Account;
import com.abadeksvp.bankingsimulator.domain.model.Currency;
import com.abadeksvp.bankingsimulator.domain.model.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class WithdrawCommandHandlerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private CommandBus commandBus;

    private Account systemAccount;

    @BeforeEach
    void setUp() {
        systemAccount = createSystemAccount(Currency.USD);
    }

    @Test
    void shouldWithdrawFromAccount() throws Exception {
        Account userAccount = createUserAccount(Currency.USD);
        fundAccount(systemAccount, userAccount, new Money(100000, Currency.USD));

        WithdrawCommand command = new WithdrawCommand(
                "withdraw-001", userAccount.getId(),
                new Money(30000, Currency.USD), "ATM withdrawal"
        );

        Result<UUID> result = commandBus.dispatch(command).get();

        assertThat(result.isSuccess()).isTrue();

        Account updated = accountRepository.findById(userAccount.getId()).orElseThrow();
        assertThat(updated.getTotalBalance()).isEqualTo(new Money(70000, Currency.USD));
        assertThat(updated.getAvailableBalance()).isEqualTo(new Money(70000, Currency.USD));

        assertZeroSum();
    }

    @Test
    void shouldReturnFailureForInsufficientFunds() throws Exception {
        Account userAccount = createUserAccount(Currency.USD);
        fundAccount(systemAccount, userAccount, new Money(10000, Currency.USD));

        WithdrawCommand command = new WithdrawCommand(
                "withdraw-002", userAccount.getId(),
                new Money(50000, Currency.USD), "Too much"
        );

        Result<UUID> result = commandBus.dispatch(command).get();

        assertThat(result).isEqualTo(Result.failure(
                new AppError(TransactionErrorCode.INSUFFICIENT_FUNDS,
                        "Insufficient funds in account %s".formatted(userAccount.getId()))
        ));
    }

    @Test
    void shouldReturnFailureForNonExistentAccount() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        WithdrawCommand command = new WithdrawCommand(
                "withdraw-003", nonExistentId,
                new Money(10000, Currency.USD), "Should fail"
        );

        Result<UUID> result = commandBus.dispatch(command).get();

        assertThat(result).isEqualTo(Result.failure(
                new AppError(AccountErrorCode.ACCOUNT_NOT_FOUND,
                        "Account with id %s not found".formatted(nonExistentId))
        ));
    }

    @Test
    void shouldReturnFailureWhenNoSystemAccount() throws Exception {
        Account gbpAccount = createUserAccount(Currency.GBP);

        WithdrawCommand command = new WithdrawCommand(
                "withdraw-004", gbpAccount.getId(),
                new Money(10000, Currency.GBP), "No system account"
        );

        Result<UUID> result = commandBus.dispatch(command).get();

        assertThat(result).isEqualTo(Result.failure(
                new AppError(TransactionErrorCode.SYSTEM_ACCOUNT_NOT_FOUND,
                        "No system account found for currency GBP")
        ));
    }

    @Test
    void shouldReturnFailureForCurrencyMismatch() throws Exception {
        createSystemAccount(Currency.GBP);
        Account usdAccount = createUserAccount(Currency.USD);
        fundAccount(systemAccount, usdAccount, new Money(50000, Currency.USD));

        WithdrawCommand command = new WithdrawCommand(
                "withdraw-005", usdAccount.getId(),
                new Money(10000, Currency.GBP), "Currency mismatch"
        );

        Result<UUID> result = commandBus.dispatch(command).get();

        assertThat(result).isEqualTo(Result.failure(
                new AppError(TransactionErrorCode.CURRENCY_MISMATCH,
                        "Source account currency USD does not match destination account currency GBP")
        ));
    }

    @Test
    void shouldHandleIdempotentWithdrawal() throws Exception {
        Account userAccount = createUserAccount(Currency.USD);
        fundAccount(systemAccount, userAccount, new Money(100000, Currency.USD));

        WithdrawCommand command = new WithdrawCommand(
                "withdraw-idempotent", userAccount.getId(),
                new Money(30000, Currency.USD), "Withdrawal"
        );

        Result<UUID> first = commandBus.dispatch(command).get();
        Result<UUID> second = commandBus.dispatch(command).get();

        assertThat(first).isEqualTo(second);

        Account updated = accountRepository.findById(userAccount.getId()).orElseThrow();
        assertThat(updated.getTotalBalance()).isEqualTo(new Money(70000, Currency.USD));
    }

    @Test
    void shouldReturnFailureWhenWithdrawingFromSystemAccount() throws Exception {
        WithdrawCommand command = new WithdrawCommand(
                "withdraw-006", systemAccount.getId(),
                new Money(10000, Currency.USD), "Withdraw from system"
        );

        Result<UUID> result = commandBus.dispatch(command).get();

        assertThat(result).isEqualTo(Result.failure(
                new AppError(TransactionErrorCode.WITHDRAWAL_NOT_ALLOWED,
                        "Withdrawals are only allowed from user accounts")
        ));
    }
}
