package com.abadeksvp.bankingsimulator.domain.command;

import com.abadeksvp.bankingsimulator.BaseIntegrationTest;
import com.abadeksvp.bankingsimulator.cqrs.command.CommandBus;
import com.abadeksvp.bankingsimulator.cqrs.core.AppError;
import com.abadeksvp.bankingsimulator.cqrs.core.Result;
import com.abadeksvp.bankingsimulator.domain.error.AccountErrorCode;
import com.abadeksvp.bankingsimulator.domain.model.Account;
import com.abadeksvp.bankingsimulator.domain.model.AccountType;
import com.abadeksvp.bankingsimulator.domain.model.Currency;
import com.abadeksvp.bankingsimulator.domain.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;
import java.util.UUID;

import static com.abadeksvp.bankingsimulator.AssertionUtils.assertEqualsIgnoringFields;
import static org.assertj.core.api.Assertions.assertThat;

class CreateAccountCommandHandlerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private CommandBus commandBus;

    @Autowired
    private AccountRepository accountRepository;

    @Test
    void shouldCreateAccount() throws Exception {
        UUID userId = UUID.randomUUID();

        CreateAccountCommand command = new CreateAccountCommand(
                userId, "ACC-100", AccountType.USER, Currency.USD, false
        );

        Result<UUID> result = commandBus.dispatch(command).get();

        assertThat(result.isSuccess()).isTrue();
        UUID accountId = ((Result.Success<UUID>) result).data();
        assertThat(accountId).isNotNull();

        Account expected = Account.builder()
                .id(accountId)
                .accountNumber("ACC-100")
                .userId(userId)
                .type(AccountType.USER)
                .currency(Currency.USD)
                .createdAt(FIXED_INSTANT)
                .updatedAt(FIXED_INSTANT)
                .build();

        Optional<Account> saved = accountRepository.findByAccountNumber("ACC-100");
        assertThat(saved).isPresent();
        assertEqualsIgnoringFields(saved.get(), expected, "isNew");
    }

    @Test
    void shouldFailWhenAccountNumberAlreadyExists() throws Exception {
        UUID userId = UUID.randomUUID();

        CreateAccountCommand command = new CreateAccountCommand(
                userId, "ACC-200", AccountType.SYSTEM, Currency.EUR, false
        );

        commandBus.dispatch(command).get();

        CreateAccountCommand duplicate = new CreateAccountCommand(
                UUID.randomUUID(), "ACC-200", AccountType.USER, Currency.EUR, false
        );

        Result<UUID> result = commandBus.dispatch(duplicate).get();

        assertThat(result).isEqualTo(Result.failure(
                new AppError(AccountErrorCode.ACCOUNT_ALREADY_EXISTS,
                        "Account with number ACC-200 already exists")
        ));
    }
}
