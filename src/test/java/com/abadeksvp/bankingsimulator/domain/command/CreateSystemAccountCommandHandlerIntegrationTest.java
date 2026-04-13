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

class CreateSystemAccountCommandHandlerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private CommandBus commandBus;

    @Autowired
    private AccountRepository accountRepository;

    @Test
    void shouldCreateSystemAccount() throws Exception {
        CreateSystemAccountCommand command = new CreateSystemAccountCommand(Currency.USD);

        Result<UUID> result = commandBus.dispatch(command).get();

        assertThat(result.isSuccess()).isTrue();
        UUID accountId = ((Result.Success<UUID>) result).data();
        assertThat(accountId).isNotNull();

        Optional<Account> saved = accountRepository.findByAccountNumber("SYSTEM-USD");
        assertThat(saved).isPresent();

        Account expected = Account.builder()
                .id(accountId)
                .accountNumber("SYSTEM-USD")
                .userId(CreateSystemAccountCommandHandler.SYSTEM_USER_ID)
                .type(AccountType.SYSTEM)
                .currency(Currency.USD)
                .overdraftEnabled(true)
                .createdAt(FIXED_INSTANT)
                .updatedAt(FIXED_INSTANT)
                .build();

        assertEqualsIgnoringFields(saved.get(), expected, "isNew");
    }

    @Test
    void shouldFailWhenAccountNumberAlreadyExists() throws Exception {
        commandBus.dispatch(new CreateSystemAccountCommand(Currency.EUR)).get();

        Result<UUID> result = commandBus.dispatch(
                new CreateSystemAccountCommand(Currency.EUR)
        ).get();

        assertThat(result).isEqualTo(Result.failure(
                new AppError(AccountErrorCode.ACCOUNT_ALREADY_EXISTS,
                        "Account with number SYSTEM-EUR already exists")
        ));
    }
}
