package com.abadeksvp.bankingsimulator.domain.command;

import com.abadeksvp.bankingsimulator.BaseIntegrationTest;
import com.abadeksvp.bankingsimulator.cqrs.command.CommandBus;
import com.abadeksvp.bankingsimulator.cqrs.core.AppError;
import com.abadeksvp.bankingsimulator.cqrs.core.Result;
import com.abadeksvp.bankingsimulator.domain.error.AccountErrorCode;
import com.abadeksvp.bankingsimulator.domain.error.TransactionErrorCode;
import com.abadeksvp.bankingsimulator.domain.model.Account;
import com.abadeksvp.bankingsimulator.domain.model.AccountType;
import com.abadeksvp.bankingsimulator.domain.model.Currency;
import com.abadeksvp.bankingsimulator.domain.model.Money;
import com.abadeksvp.bankingsimulator.domain.repository.AccountRepository;
import com.abadeksvp.bankingsimulator.domain.service.ProcessTransactionRequest;
import com.abadeksvp.bankingsimulator.domain.service.TransactionProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class TransferCommandHandlerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private CommandBus commandBus;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionProcessor transactionProcessor;

    private final AtomicInteger idempotencyCounter = new AtomicInteger();
    private Account systemAccount;

    @BeforeEach
    void setUp() {
        systemAccount = createSystemAccount(Currency.USD);
    }

    @Test
    void shouldTransferBetweenAccounts() throws Exception {
        Account source = createUserAccount(Currency.USD);
        Account destination = createUserAccount(Currency.USD);
        fundAccount(source, new Money(100000, Currency.USD));

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
        fundAccount(source, new Money(10000, Currency.USD));

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
    void shouldReturnFailureForNonExistentAccount() throws Exception {
        Account source = createUserAccount(Currency.USD);
        UUID nonExistentId = UUID.randomUUID();

        TransferCommand command = new TransferCommand(
                "transfer-003", source.getId(), nonExistentId,
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
        fundAccount(source, new Money(50000, Currency.USD));

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

    private Account createSystemAccount(Currency currency) {
        Instant now = clock.now();
        Account account = Account.builder()
                .id(UUID.randomUUID())
                .accountNumber("SYSTEM-" + currency.name())
                .userId(CreateSystemAccountCommandHandler.SYSTEM_USER_ID)
                .type(AccountType.SYSTEM)
                .currency(currency)
                .overdraftEnabled(true)
                .createdAt(now)
                .updatedAt(now)
                .build();
        accountRepository.save(account);
        return account;
    }

    private Account createUserAccount(Currency currency) {
        Instant now = clock.now();
        Account account = Account.builder()
                .id(UUID.randomUUID())
                .accountNumber("ACC-" + UUID.randomUUID().toString().substring(0, 8))
                .userId(UUID.randomUUID())
                .type(AccountType.USER)
                .currency(currency)
                .createdAt(now)
                .updatedAt(now)
                .build();
        accountRepository.save(account);
        return account;
    }

    private void fundAccount(Account userAccount, Money amount) {
        transactionProcessor.processAtomically(new ProcessTransactionRequest(
                "fund-" + idempotencyCounter.incrementAndGet(),
                systemAccount.getId(),
                userAccount.getId(),
                amount,
                "Test funding"
        ));
    }

    private void assertZeroSum() {
        long sum = 0;
        for (Account account : accountRepository.findAll()) {
            sum += account.getTotalBalance().amount();
        }
        assertThat(sum).isZero();
    }
}
