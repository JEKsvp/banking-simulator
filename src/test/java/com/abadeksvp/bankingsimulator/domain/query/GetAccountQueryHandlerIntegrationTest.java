package com.abadeksvp.bankingsimulator.domain.query;

import com.abadeksvp.bankingsimulator.BaseIntegrationTest;
import com.abadeksvp.bankingsimulator.cqrs.core.AppError;
import com.abadeksvp.bankingsimulator.cqrs.core.Result;
import com.abadeksvp.bankingsimulator.cqrs.query.QueryBus;
import com.abadeksvp.bankingsimulator.domain.error.AccountErrorCode;
import com.abadeksvp.bankingsimulator.domain.model.Account;
import com.abadeksvp.bankingsimulator.domain.model.AccountType;
import com.abadeksvp.bankingsimulator.domain.model.Currency;
import com.abadeksvp.bankingsimulator.domain.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.UUID;

import static com.abadeksvp.bankingsimulator.AssertionUtils.assertEqualsIgnoringFields;
import static org.assertj.core.api.Assertions.assertThat;

class GetAccountQueryHandlerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private QueryBus queryBus;

    @Autowired
    private AccountRepository accountRepository;

    @Test
    void shouldReturnAccount() throws Exception {
        UUID accountId = UUID.randomUUID();
        Instant now = clock.now();

        Account account = Account.builder()
                .id(accountId)
                .accountNumber("ACC-300")
                .userId(UUID.randomUUID())
                .type(AccountType.USER)
                .currency(Currency.USD)
                .totalBalance(250000)
                .availableBalance(250000)
                .createdAt(now)
                .updatedAt(now)
                .build();

        accountRepository.save(account);

        Result<Account> result = queryBus.dispatch(new GetAccountQuery(accountId)).get();

        assertEqualsIgnoringFields(result, Result.success(account), "data.isNew");
    }

    @Test
    void shouldReturnFailureWhenAccountNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        Result<Account> result = queryBus.dispatch(new GetAccountQuery(nonExistentId)).get();

        assertThat(result).isEqualTo(Result.failure(
                new AppError(AccountErrorCode.ACCOUNT_NOT_FOUND,
                        "Account with id %s not found".formatted(nonExistentId))
        ));
    }
}
