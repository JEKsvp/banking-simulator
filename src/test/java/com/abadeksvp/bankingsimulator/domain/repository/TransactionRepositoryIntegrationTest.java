package com.abadeksvp.bankingsimulator.domain.repository;

import com.abadeksvp.bankingsimulator.BaseIntegrationTest;
import com.abadeksvp.bankingsimulator.domain.model.Account;
import com.abadeksvp.bankingsimulator.domain.model.AccountType;
import com.abadeksvp.bankingsimulator.domain.model.Currency;
import com.abadeksvp.bankingsimulator.domain.model.Money;
import com.abadeksvp.bankingsimulator.domain.model.Transaction;
import com.abadeksvp.bankingsimulator.domain.model.TransactionStatus;
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

    private Account sourceAccount;
    private Account destinationAccount;

    @BeforeEach
    void setUp() {
        Instant now = clock.now();

        sourceAccount = Account.builder()
                .id(UUID.randomUUID())
                .accountNumber("SRC-001")
                .userId(UUID.randomUUID())
                .type(AccountType.USER)
                .currency(Currency.USD)
                .createdAt(now)
                .updatedAt(now)
                .build();

        destinationAccount = Account.builder()
                .id(UUID.randomUUID())
                .accountNumber("DST-001")
                .userId(UUID.randomUUID())
                .type(AccountType.SYSTEM)
                .currency(Currency.USD)
                .createdAt(now)
                .updatedAt(now)
                .build();

        accountRepository.save(sourceAccount);
        accountRepository.save(destinationAccount);
    }

    @Test
    void shouldSaveAndFindTransactionById() {
        Instant now = clock.now();

        Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID())
                .status(TransactionStatus.PENDING)
                .sourceAccountId(sourceAccount.getId())
                .destinationAccountId(destinationAccount.getId())
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
                .status(TransactionStatus.PENDING)
                .sourceAccountId(sourceAccount.getId())
                .destinationAccountId(destinationAccount.getId())
                .amount(new Money(5000, Currency.USD))
                .idempotencyKey("txn-002")
                .createdAt(now)
                .updatedAt(now)
                .build();

        Transaction t2 = Transaction.builder()
                .id(UUID.randomUUID())
                .status(TransactionStatus.PENDING)
                .sourceAccountId(destinationAccount.getId())
                .destinationAccountId(sourceAccount.getId())
                .amount(new Money(20000, Currency.USD))
                .idempotencyKey("txn-003")
                .createdAt(now)
                .updatedAt(now)
                .build();

        transactionRepository.save(t1);
        transactionRepository.save(t2);

        List<Transaction> found = transactionRepository.findBySourceAccountIdOrDestinationAccountId(
                sourceAccount.getId(), sourceAccount.getId()
        );
        assertThat(found).hasSize(2);
    }

    @Test
    void shouldRejectDuplicateIdempotencyKey() {
        Instant now = clock.now();

        Transaction t1 = Transaction.builder()
                .id(UUID.randomUUID())
                .status(TransactionStatus.PENDING)
                .sourceAccountId(sourceAccount.getId())
                .destinationAccountId(destinationAccount.getId())
                .amount(new Money(10000, Currency.USD))
                .idempotencyKey("duplicate-key")
                .createdAt(now)
                .updatedAt(now)
                .build();

        Transaction t2 = Transaction.builder()
                .id(UUID.randomUUID())
                .status(TransactionStatus.PENDING)
                .sourceAccountId(sourceAccount.getId())
                .destinationAccountId(destinationAccount.getId())
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
                .status(TransactionStatus.PENDING)
                .sourceAccountId(sourceAccount.getId())
                .destinationAccountId(destinationAccount.getId())
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
