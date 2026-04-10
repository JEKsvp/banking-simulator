package com.abadeksvp.bankingsimulator.domain.repository;

import com.abadeksvp.bankingsimulator.BaseIntegrationTest;
import com.abadeksvp.bankingsimulator.domain.model.Account;
import com.abadeksvp.bankingsimulator.domain.model.AccountType;
import com.abadeksvp.bankingsimulator.domain.model.Currency;
import com.abadeksvp.bankingsimulator.domain.model.Money;
import com.abadeksvp.bankingsimulator.domain.model.Transaction;
import com.abadeksvp.bankingsimulator.domain.model.TransactionStatus;
import com.abadeksvp.bankingsimulator.domain.model.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionRepositoryIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    private Account billAccount;
    private Account counterpartAccount;

    @BeforeEach
    void setUp() {
        Instant now = clock.now();

        billAccount = Account.builder()
                .id(UUID.randomUUID())
                .accountNumber("BILL-001")
                .userId(UUID.randomUUID())
                .type(AccountType.USER)
                .balance(new Money(new BigDecimal("1000.00"), Currency.USD))
                .createdAt(now)
                .updatedAt(now)
                .build();

        counterpartAccount = Account.builder()
                .id(UUID.randomUUID())
                .accountNumber("CNTP-001")
                .userId(UUID.randomUUID())
                .type(AccountType.COMPANY)
                .balance(new Money(new BigDecimal("5000.00"), Currency.USD))
                .createdAt(now)
                .updatedAt(now)
                .build();

        accountRepository.save(billAccount);
        accountRepository.save(counterpartAccount);
    }

    @Test
    void shouldSaveAndFindTransactionById() {
        Instant now = clock.now();

        Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID())
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.CREATED)
                .billAccountId(billAccount.getId())
                .counterpartAccountId(counterpartAccount.getId())
                .billingAmount(new Money(new BigDecimal("100.00"), Currency.USD))
                .transactionAmount(new Money(new BigDecimal("100.00"), Currency.USD))
                .description("Test transfer")
                .createdAt(now)
                .updatedAt(now)
                .build();

        transactionRepository.save(transaction);

        assertThat(transactionRepository.findById(transaction.getId()))
                .isPresent()
                .contains(transaction);
    }

    @Test
    void shouldFindTransactionsByAccountId() {
        Instant now = clock.now();

        Transaction t1 = Transaction.builder()
                .id(UUID.randomUUID())
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.CREATED)
                .billAccountId(billAccount.getId())
                .counterpartAccountId(counterpartAccount.getId())
                .billingAmount(new Money(new BigDecimal("50.00"), Currency.USD))
                .transactionAmount(new Money(new BigDecimal("50.00"), Currency.USD))
                .createdAt(now)
                .updatedAt(now)
                .build();

        Transaction t2 = Transaction.builder()
                .id(UUID.randomUUID())
                .type(TransactionType.DEPOSIT)
                .status(TransactionStatus.CREATED)
                .billAccountId(counterpartAccount.getId())
                .counterpartAccountId(billAccount.getId())
                .billingAmount(new Money(new BigDecimal("200.00"), Currency.USD))
                .transactionAmount(new Money(new BigDecimal("200.00"), Currency.USD))
                .createdAt(now)
                .updatedAt(now)
                .build();

        transactionRepository.save(t1);
        transactionRepository.save(t2);

        List<Transaction> found = transactionRepository.findByBillAccountIdOrCounterpartAccountId(
                billAccount.getId(), billAccount.getId()
        );
        assertThat(found).hasSize(2);
    }
}
