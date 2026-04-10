package com.abadeksvp.bankingsimulator.domain.repository;

import com.abadeksvp.bankingsimulator.BaseIntegrationTest;
import com.abadeksvp.bankingsimulator.domain.model.Account;
import com.abadeksvp.bankingsimulator.domain.model.AccountType;
import com.abadeksvp.bankingsimulator.domain.model.Currency;
import com.abadeksvp.bankingsimulator.domain.model.Money;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AccountRepositoryIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private AccountRepository accountRepository;

    @Test
    void shouldSaveAndFindAccountById() {
        UUID id = UUID.randomUUID();
        Instant now = clock.now();

        Account account = Account.builder()
                .id(id)
                .accountNumber("ACC-001")
                .userId(UUID.randomUUID())
                .type(AccountType.USER)
                .balance(new Money(new BigDecimal("1000.00"), Currency.USD))
                .createdAt(now)
                .updatedAt(now)
                .build();

        accountRepository.save(account);

        Optional<Account> found = accountRepository.findById(id);
        assertThat(found).isPresent();
        assertThat(found.get()).isEqualTo(account);
    }

    @Test
    void shouldFindAccountByAccountNumber() {
        UUID id = UUID.randomUUID();
        Instant now = clock.now();

        Account account = Account.builder()
                .id(id)
                .accountNumber("ACC-001")
                .userId(UUID.randomUUID())
                .type(AccountType.COMPANY)
                .balance(new Money(new BigDecimal("5000.00"), Currency.EUR))
                .createdAt(now)
                .updatedAt(now)
                .build();

        accountRepository.save(account);

        Optional<Account> found = accountRepository.findByAccountNumber("ACC-001");
        assertThat(found).isPresent();
        assertThat(found.get()).isEqualTo(account);
    }

    @Test
    void shouldReturnEmptyForNonExistentAccountNumber() {
        Optional<Account> found = accountRepository.findByAccountNumber("NON-EXISTENT");
        assertThat(found).isEmpty();
    }
}
