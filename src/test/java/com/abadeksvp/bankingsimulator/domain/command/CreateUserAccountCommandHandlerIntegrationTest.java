package com.abadeksvp.bankingsimulator.domain.command;

import com.abadeksvp.bankingsimulator.BaseIntegrationTest;
import com.abadeksvp.bankingsimulator.cqrs.command.CommandBus;
import com.abadeksvp.bankingsimulator.cqrs.core.AppError;
import com.abadeksvp.bankingsimulator.cqrs.core.Result;
import com.abadeksvp.bankingsimulator.domain.error.TransactionErrorCode;
import com.abadeksvp.bankingsimulator.domain.model.Account;
import com.abadeksvp.bankingsimulator.domain.model.AccountType;
import com.abadeksvp.bankingsimulator.domain.model.Currency;
import com.abadeksvp.bankingsimulator.domain.model.Money;
import com.abadeksvp.bankingsimulator.domain.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CreateUserAccountCommandHandlerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private CommandBus commandBus;

    @Autowired
    private AccountRepository accountRepository;

    @Test
    void shouldCreateAccount() throws Exception {
        UUID userId = UUID.randomUUID();

        CreateUserAccountCommand command = new CreateUserAccountCommand(
                userId, Currency.USD, false, null
        );

        Result<UUID> result = commandBus.dispatch(command).get();

        assertThat(result.isSuccess()).isTrue();
        UUID accountId = ((Result.Success<UUID>) result).data();
        assertThat(accountId).isNotNull();

        Account saved = accountRepository.findById(accountId).orElseThrow();
        assertThat(saved.getAccountNumber()).isEqualTo("ACC-" + accountId);
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getType()).isEqualTo(AccountType.USER);
        assertThat(saved.getCurrency()).isEqualTo(Currency.USD);
        assertThat(saved.isOverdraftEnabled()).isFalse();
        assertThat(saved.getCreatedAt()).isEqualTo(FIXED_INSTANT);
        assertThat(saved.getUpdatedAt()).isEqualTo(FIXED_INSTANT);
    }

    @Test
    void shouldCreateAccountWithInitialDeposit() throws Exception {
        commandBus.dispatch(new CreateSystemAccountCommand(Currency.USD)).get();

        UUID userId = UUID.randomUUID();
        CreateUserAccountCommand command = new CreateUserAccountCommand(
                userId, Currency.USD, false,
                new Money(50000, Currency.USD)
        );

        Result<UUID> result = commandBus.dispatch(command).get();

        assertThat(result.isSuccess()).isTrue();
        UUID accountId = ((Result.Success<UUID>) result).data();

        Account userAccount = accountRepository.findById(accountId).orElseThrow();
        assertThat(userAccount.getTotalBalance()).isEqualTo(new Money(50000, Currency.USD));
        assertThat(userAccount.getAvailableBalance()).isEqualTo(new Money(50000, Currency.USD));

        assertZeroSum();
    }

    @Test
    void shouldFailInitialDepositWhenNoSystemAccount() throws Exception {
        UUID userId = UUID.randomUUID();
        CreateUserAccountCommand command = new CreateUserAccountCommand(
                userId, Currency.GBP, false,
                new Money(10000, Currency.GBP)
        );

        Result<UUID> result = commandBus.dispatch(command).get();

        assertThat(result).isEqualTo(Result.failure(
                new AppError(TransactionErrorCode.SYSTEM_ACCOUNT_NOT_FOUND,
                        "No system account found for currency GBP")
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
