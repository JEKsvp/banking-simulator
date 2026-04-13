package com.abadeksvp.bankingsimulator;

import com.abadeksvp.bankingsimulator.common.Clock;
import com.abadeksvp.bankingsimulator.common.FixedClock;
import com.abadeksvp.bankingsimulator.domain.command.CreateSystemAccountCommandHandler;
import com.abadeksvp.bankingsimulator.domain.model.Account;
import com.abadeksvp.bankingsimulator.domain.model.AccountType;
import com.abadeksvp.bankingsimulator.domain.model.Currency;
import com.abadeksvp.bankingsimulator.domain.model.Money;
import com.abadeksvp.bankingsimulator.domain.repository.AccountRepository;
import com.abadeksvp.bankingsimulator.domain.service.ProcessTransactionRequest;
import com.abadeksvp.bankingsimulator.domain.service.TransactionProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.jdbc.Sql;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@Import({TestcontainersConfiguration.class, BaseIntegrationTest.TestClockConfiguration.class})
@SpringBootTest
@Sql(statements = "TRUNCATE accounts, transactions CASCADE", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public abstract class BaseIntegrationTest {

    protected static final Instant FIXED_INSTANT = Instant.parse("2025-01-15T10:00:00Z");

    private final AtomicInteger idempotencyCounter = new AtomicInteger();

    @Autowired
    protected Clock clock;

    @Autowired
    protected AccountRepository accountRepository;

    @Autowired
    protected TransactionProcessor transactionProcessor;

    @TestConfiguration
    static class TestClockConfiguration {

        @Bean
        @Primary
        public Clock fixedClock() {
            return new FixedClock(FIXED_INSTANT);
        }
    }

    protected Account createSystemAccount(Currency currency) {
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

    protected Account createUserAccount(Currency currency) {
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

    protected void fundAccount(Account systemAccount, Account userAccount, Money amount) {
        transactionProcessor.processAtomically(new ProcessTransactionRequest(
                "fund-" + idempotencyCounter.incrementAndGet(),
                systemAccount.getId(),
                userAccount.getId(),
                amount,
                "Test funding"
        ));
    }

    protected void assertZeroSum() {
        long sum = 0;
        for (Account account : accountRepository.findAll()) {
            sum += account.getTotalBalance().amount();
        }
        assertThat(sum).isZero();
    }
}
