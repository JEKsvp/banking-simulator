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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DepositCommandHandlerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private CommandBus commandBus;

    @Autowired
    private AccountRepository accountRepository;

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

    private void assertZeroSum() {
        long sum = 0;
        for (Account account : accountRepository.findAll()) {
            sum += account.getTotalBalance().amount();
        }
        assertThat(sum).isZero();
    }
}
