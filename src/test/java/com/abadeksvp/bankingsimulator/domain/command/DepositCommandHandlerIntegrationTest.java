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

class DepositCommandHandlerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private CommandBus commandBus;

    private Account systemAccount;

    @BeforeEach
    void setUp() {
        systemAccount = createSystemAccount(Currency.USD);
    }

    @Test
    void shouldDepositToAccount() throws Exception {
        Account userAccount = createUserAccount(Currency.USD);

        DepositCommand command = new DepositCommand(
                "deposit-001", userAccount.getId(),
                new Money(50000, Currency.USD), "Salary"
        );

        Result<UUID> result = commandBus.dispatch(command).get();

        assertThat(result.isSuccess()).isTrue();
        UUID transactionId = ((Result.Success<UUID>) result).data();
        assertThat(transactionId).isNotNull();

        Account updated = accountRepository.findById(userAccount.getId()).orElseThrow();
        assertThat(updated.getTotalBalance()).isEqualTo(new Money(50000, Currency.USD));
        assertThat(updated.getAvailableBalance()).isEqualTo(new Money(50000, Currency.USD));

        assertZeroSum();
    }

    @Test
    void shouldReturnFailureForNonExistentAccount() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        DepositCommand command = new DepositCommand(
                "deposit-002", nonExistentId,
                new Money(10000, Currency.USD), "Should fail"
        );

        Result<UUID> result = commandBus.dispatch(command).get();

        assertThat(result).isEqualTo(Result.failure(
                new AppError(AccountErrorCode.ACCOUNT_NOT_FOUND,
                        "Account with id %s not found".formatted(nonExistentId))
        ));
    }

    @Test
    void shouldReturnFailureForCurrencyMismatch() throws Exception {
        Account eurAccount = createUserAccount(Currency.EUR);

        DepositCommand command = new DepositCommand(
                "deposit-003", eurAccount.getId(),
                new Money(10000, Currency.USD), "Currency mismatch"
        );

        Result<UUID> result = commandBus.dispatch(command).get();

        assertThat(result).isEqualTo(Result.failure(
                new AppError(TransactionErrorCode.CURRENCY_MISMATCH,
                        "Source account currency USD does not match destination account currency EUR")
        ));
    }

    @Test
    void shouldReturnFailureWhenNoSystemAccount() throws Exception {
        Account gbpAccount = createUserAccount(Currency.GBP);

        DepositCommand command = new DepositCommand(
                "deposit-004", gbpAccount.getId(),
                new Money(10000, Currency.GBP), "No system account"
        );

        Result<UUID> result = commandBus.dispatch(command).get();

        assertThat(result).isEqualTo(Result.failure(
                new AppError(TransactionErrorCode.SYSTEM_ACCOUNT_NOT_FOUND,
                        "No system account found for currency GBP")
        ));
    }

    @Test
    void shouldHandleIdempotentDeposit() throws Exception {
        Account userAccount = createUserAccount(Currency.USD);

        DepositCommand command = new DepositCommand(
                "deposit-idempotent", userAccount.getId(),
                new Money(50000, Currency.USD), "Deposit"
        );

        Result<UUID> first = commandBus.dispatch(command).get();
        Result<UUID> second = commandBus.dispatch(command).get();

        assertThat(first).isEqualTo(second);

        Account updated = accountRepository.findById(userAccount.getId()).orElseThrow();
        assertThat(updated.getTotalBalance()).isEqualTo(new Money(50000, Currency.USD));
    }

    @Test
    void shouldReturnFailureWhenDepositingToSystemAccount() throws Exception {
        DepositCommand command = new DepositCommand(
                "deposit-005", systemAccount.getId(),
                new Money(10000, Currency.USD), "Deposit to system"
        );

        Result<UUID> result = commandBus.dispatch(command).get();

        assertThat(result).isEqualTo(Result.failure(
                new AppError(TransactionErrorCode.DEPOSIT_NOT_ALLOWED,
                        "Deposits are only allowed to user accounts")
        ));
    }
}
