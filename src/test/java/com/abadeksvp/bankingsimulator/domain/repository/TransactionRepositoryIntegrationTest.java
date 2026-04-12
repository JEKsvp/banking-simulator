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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.dao.DuplicateKeyException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
                .currency(Currency.USD)
                .totalBalance(100000)
                .availableBalance(100000)
                .createdAt(now)
                .updatedAt(now)
                .build();

        counterpartAccount = Account.builder()
                .id(UUID.randomUUID())
                .accountNumber("CNTP-001")
                .userId(UUID.randomUUID())
                .type(AccountType.SYSTEM)
                .currency(Currency.USD)
                .totalBalance(500000)
                .availableBalance(500000)
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
                .status(TransactionStatus.PENDING)
                .billAccountId(billAccount.getId())
                .counterpartAccountId(counterpartAccount.getId())
                .amount(new Money(10000, Currency.USD))
                .idempotencyKey("txn-001")
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
                .status(TransactionStatus.PENDING)
                .billAccountId(billAccount.getId())
                .counterpartAccountId(counterpartAccount.getId())
                .amount(new Money(5000, Currency.USD))
                .idempotencyKey("txn-002")
                .createdAt(now)
                .updatedAt(now)
                .build();

        Transaction t2 = Transaction.builder()
                .id(UUID.randomUUID())
                .type(TransactionType.DEPOSIT)
                .status(TransactionStatus.PENDING)
                .billAccountId(counterpartAccount.getId())
                .counterpartAccountId(billAccount.getId())
                .amount(new Money(20000, Currency.USD))
                .idempotencyKey("txn-003")
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

    @Test
    void shouldRejectDuplicateIdempotencyKey() {
        Instant now = clock.now();

        Transaction t1 = Transaction.builder()
                .id(UUID.randomUUID())
                .type(TransactionType.DEPOSIT)
                .status(TransactionStatus.PENDING)
                .billAccountId(billAccount.getId())
                .counterpartAccountId(counterpartAccount.getId())
                .amount(new Money(10000, Currency.USD))
                .idempotencyKey("duplicate-key")
                .createdAt(now)
                .updatedAt(now)
                .build();

        Transaction t2 = Transaction.builder()
                .id(UUID.randomUUID())
                .type(TransactionType.DEPOSIT)
                .status(TransactionStatus.PENDING)
                .billAccountId(billAccount.getId())
                .counterpartAccountId(counterpartAccount.getId())
                .amount(new Money(5000, Currency.USD))
                .idempotencyKey("duplicate-key")
                .createdAt(now)
                .updatedAt(now)
                .build();

        transactionRepository.save(t1);

        assertThatThrownBy(() -> transactionRepository.save(t2))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void shouldFindTransactionByIdempotencyKey() {
        Instant now = clock.now();

        Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID())
                .type(TransactionType.DEPOSIT)
                .status(TransactionStatus.PENDING)
                .billAccountId(billAccount.getId())
                .counterpartAccountId(counterpartAccount.getId())
                .amount(new Money(10000, Currency.USD))
                .idempotencyKey("unique-key-001")
                .createdAt(now)
                .updatedAt(now)
                .build();

        transactionRepository.save(transaction);

        assertThat(transactionRepository.findByIdempotencyKey("unique-key-001"))
                .isPresent()
                .contains(transaction);

        assertThat(transactionRepository.findByIdempotencyKey("non-existent"))
                .isEmpty();
    }
}
