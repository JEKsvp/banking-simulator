package com.abadeksvp.bankingsimulator.domain.query;

import com.abadeksvp.bankingsimulator.BaseIntegrationTest;
import com.abadeksvp.bankingsimulator.cqrs.core.AppError;
import com.abadeksvp.bankingsimulator.cqrs.core.Result;
import com.abadeksvp.bankingsimulator.cqrs.query.QueryBus;
import com.abadeksvp.bankingsimulator.domain.error.AccountErrorCode;
import com.abadeksvp.bankingsimulator.domain.model.Account;
import com.abadeksvp.bankingsimulator.domain.model.AccountType;
import com.abadeksvp.bankingsimulator.domain.model.Currency;
import com.abadeksvp.bankingsimulator.domain.model.Money;
import com.abadeksvp.bankingsimulator.domain.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GetAccountQueryHandlerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private QueryBus queryBus;

    @Autowired
    private AccountRepository accountRepository;

    @Test
    void shouldReturnAccountView() throws Exception {
        UUID accountId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = clock.now();

        Account account = Account.builder()
                .id(accountId)
                .accountNumber("ACC-300")
                .userId(userId)
                .type(AccountType.USER)
                .currency(Currency.USD)
                .createdAt(now)
                .updatedAt(now)
                .build();

        accountRepository.save(account);

        Result<AccountView> result = queryBus.dispatch(new GetAccountQuery(accountId)).get();

        AccountView expected = new AccountView(
                accountId,
                "ACC-300",
                userId,
                AccountType.USER,
                Currency.USD,
                new Money(0, Currency.USD),
                new Money(0, Currency.USD),
                false,
                now,
                now
        );

        assertThat(result).isEqualTo(Result.success(expected));
    }

    @Test
    void shouldReturnFailureWhenAccountNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        Result<AccountView> result = queryBus.dispatch(new GetAccountQuery(nonExistentId)).get();

        assertThat(result).isEqualTo(Result.failure(
                new AppError(AccountErrorCode.ACCOUNT_NOT_FOUND,
                        "Account with id %s not found".formatted(nonExistentId))
        ));
    }
}
