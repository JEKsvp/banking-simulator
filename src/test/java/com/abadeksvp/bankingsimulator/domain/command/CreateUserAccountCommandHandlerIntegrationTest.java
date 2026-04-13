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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static com.abadeksvp.bankingsimulator.AssertionUtils.assertEqualsIgnoringFields;
import static org.assertj.core.api.Assertions.assertThat;

class CreateUserAccountCommandHandlerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private CommandBus commandBus;

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

        Account expected = Account.builder()
                .id(accountId)
                .accountNumber("ACC-" + accountId)
                .userId(userId)
                .type(AccountType.USER)
                .currency(Currency.USD)
                .overdraftEnabled(false)
                .createdAt(FIXED_INSTANT)
                .updatedAt(FIXED_INSTANT)
                .build();

        assertEqualsIgnoringFields(saved, expected, "isNew");
        assertThat(saved.getTotalBalance()).isEqualTo(new Money(0, Currency.USD));
        assertThat(saved.getAvailableBalance()).isEqualTo(new Money(0, Currency.USD));
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

        assertThat(accountRepository.findAll()).isEmpty();
    }
}
