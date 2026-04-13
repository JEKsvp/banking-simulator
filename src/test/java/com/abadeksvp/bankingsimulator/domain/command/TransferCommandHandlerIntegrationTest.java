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

class TransferCommandHandlerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private CommandBus commandBus;

    private Account systemAccount;

    @BeforeEach
    void setUp() {
        systemAccount = createSystemAccount(Currency.USD);
    }

    @Test
    void shouldTransferBetweenAccounts() throws Exception {
        Account source = createUserAccount(Currency.USD);
        Account destination = createUserAccount(Currency.USD);
        fundAccount(systemAccount, source, new Money(100000, Currency.USD));

        TransferCommand command = new TransferCommand(
                "transfer-001", source.getId(), destination.getId(),
                new Money(30000, Currency.USD), "Payment"
        );

        Result<UUID> result = commandBus.dispatch(command).get();

        assertThat(result.isSuccess()).isTrue();

        Account updatedSource = accountRepository.findById(source.getId()).orElseThrow();
        assertThat(updatedSource.getTotalBalance()).isEqualTo(new Money(70000, Currency.USD));
        assertThat(updatedSource.getAvailableBalance()).isEqualTo(new Money(70000, Currency.USD));

        Account updatedDestination = accountRepository.findById(destination.getId()).orElseThrow();
        assertThat(updatedDestination.getTotalBalance()).isEqualTo(new Money(30000, Currency.USD));
        assertThat(updatedDestination.getAvailableBalance()).isEqualTo(new Money(30000, Currency.USD));

        assertZeroSum();
    }

    @Test
    void shouldReturnFailureForInsufficientFunds() throws Exception {
        Account source = createUserAccount(Currency.USD);
        Account destination = createUserAccount(Currency.USD);
        fundAccount(systemAccount, source, new Money(10000, Currency.USD));

        TransferCommand command = new TransferCommand(
                "transfer-002", source.getId(), destination.getId(),
                new Money(50000, Currency.USD), "Too much"
        );

        Result<UUID> result = commandBus.dispatch(command).get();

        assertThat(result).isEqualTo(Result.failure(
                new AppError(TransactionErrorCode.INSUFFICIENT_FUNDS,
                        "Insufficient funds in account %s".formatted(source.getId()))
        ));
    }

    @Test
    void shouldReturnFailureForNonExistentSource() throws Exception {
        Account destination = createUserAccount(Currency.USD);
        UUID nonExistentId = UUID.randomUUID();

        TransferCommand command = new TransferCommand(
                "transfer-003a", nonExistentId, destination.getId(),
                new Money(10000, Currency.USD), "Should fail"
        );

        Result<UUID> result = commandBus.dispatch(command).get();

        assertThat(result).isEqualTo(Result.failure(
                new AppError(AccountErrorCode.ACCOUNT_NOT_FOUND,
                        "Account with id %s not found".formatted(nonExistentId))
        ));
    }

    @Test
    void shouldReturnFailureForNonExistentDestination() throws Exception {
        Account source = createUserAccount(Currency.USD);
        UUID nonExistentId = UUID.randomUUID();

        TransferCommand command = new TransferCommand(
                "transfer-003b", source.getId(), nonExistentId,
                new Money(10000, Currency.USD), "Should fail"
        );

        Result<UUID> result = commandBus.dispatch(command).get();

        assertThat(result).isEqualTo(Result.failure(
                new AppError(AccountErrorCode.ACCOUNT_NOT_FOUND,
                        "Account with id %s not found".formatted(nonExistentId))
        ));
    }

    @Test
    void shouldReturnFailureWhenSourceIsSystemAccount() throws Exception {
        Account destination = createUserAccount(Currency.USD);

        TransferCommand command = new TransferCommand(
                "transfer-005", systemAccount.getId(), destination.getId(),
                new Money(10000, Currency.USD), "System transfer"
        );

        Result<UUID> result = commandBus.dispatch(command).get();

        assertThat(result).isEqualTo(Result.failure(
                new AppError(TransactionErrorCode.TRANSFER_NOT_ALLOWED,
                        "Transfers are only allowed between user accounts")
        ));
    }

    @Test
    void shouldReturnFailureWhenDestinationIsSystemAccount() throws Exception {
        Account source = createUserAccount(Currency.USD);
        fundAccount(systemAccount, source, new Money(50000, Currency.USD));

        TransferCommand command = new TransferCommand(
                "transfer-006", source.getId(), systemAccount.getId(),
                new Money(10000, Currency.USD), "System transfer"
        );

        Result<UUID> result = commandBus.dispatch(command).get();

        assertThat(result).isEqualTo(Result.failure(
                new AppError(TransactionErrorCode.TRANSFER_NOT_ALLOWED,
                        "Transfers are only allowed between user accounts")
        ));
    }

    @Test
    void shouldReturnFailureForCurrencyMismatch() throws Exception {
        Account source = createUserAccount(Currency.USD);
        Account destination = createUserAccount(Currency.EUR);

        TransferCommand command = new TransferCommand(
                "transfer-004", source.getId(), destination.getId(),
                new Money(10000, Currency.USD), "Currency mismatch"
        );

        Result<UUID> result = commandBus.dispatch(command).get();

        assertThat(result).isEqualTo(Result.failure(
                new AppError(TransactionErrorCode.CURRENCY_MISMATCH,
                        "Source account currency USD does not match destination account currency EUR")
        ));
    }

    @Test
    void shouldHandleIdempotentTransfer() throws Exception {
        Account source = createUserAccount(Currency.USD);
        Account destination = createUserAccount(Currency.USD);
        fundAccount(systemAccount, source, new Money(100000, Currency.USD));

        TransferCommand command = new TransferCommand(
                "transfer-idempotent", source.getId(), destination.getId(),
                new Money(30000, Currency.USD), "Payment"
        );

        Result<UUID> first = commandBus.dispatch(command).get();
        Result<UUID> second = commandBus.dispatch(command).get();

        assertThat(first).isEqualTo(second);

        Account updatedSource = accountRepository.findById(source.getId()).orElseThrow();
        assertThat(updatedSource.getTotalBalance()).isEqualTo(new Money(70000, Currency.USD));
    }
}
