package com.abadeksvp.bankingsimulator.domain.service;

import com.abadeksvp.bankingsimulator.BaseIntegrationTest;
import com.abadeksvp.bankingsimulator.domain.error.AccountNotFoundException;
import com.abadeksvp.bankingsimulator.domain.error.CurrencyMismatchException;
import com.abadeksvp.bankingsimulator.domain.error.InsufficientFundsException;
import com.abadeksvp.bankingsimulator.domain.error.TransactionNotFoundException;
import com.abadeksvp.bankingsimulator.domain.model.Account;
import com.abadeksvp.bankingsimulator.domain.model.AccountType;
import com.abadeksvp.bankingsimulator.domain.model.Currency;
import com.abadeksvp.bankingsimulator.domain.model.Money;
import com.abadeksvp.bankingsimulator.domain.model.Transaction;
import com.abadeksvp.bankingsimulator.domain.model.TransactionStatus;
import com.abadeksvp.bankingsimulator.domain.repository.AccountRepository;
import com.abadeksvp.bankingsimulator.domain.repository.TransactionRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static com.abadeksvp.bankingsimulator.AssertionUtils.assertEqualsIgnoringFields;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionProcessorIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TransactionProcessor transactionProcessor;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private final AtomicInteger idempotencyCounter = new AtomicInteger();

    @Nested
    class ProcessAtomically {

        @Test
        void shouldProcessTransaction() {
            Account source = createAccount(AccountType.SYSTEM, Currency.USD, true);
            Account destination = createAccount(AccountType.USER, Currency.USD, false);

            ProcessTransactionRequest request = new ProcessTransactionRequest(
                    "deposit-001",
                    source.getId(),
                    destination.getId(),
                    new Money(50000, Currency.USD),
                    "Initial deposit"
            );

            Transaction transaction = transactionProcessor.processAtomically(request);

            Transaction expected = Transaction.builder()
                    .id(transaction.getId())
                    .status(TransactionStatus.COMPLETED)
                    .sourceAccountId(source.getId())
                    .destinationAccountId(destination.getId())
                    .amount(new Money(50000, Currency.USD))
                    .idempotencyKey("deposit-001")
                    .description("Initial deposit")
                    .createdAt(FIXED_INSTANT)
                    .updatedAt(FIXED_INSTANT)
                    .build();
            assertEqualsIgnoringFields(transaction, expected, "isNew");

            Account updatedSource = accountRepository.findById(source.getId()).orElseThrow();
            assertThat(updatedSource.getTotalBalance()).isEqualTo(new Money(-50000, Currency.USD));
            assertThat(updatedSource.getAvailableBalance()).isEqualTo(new Money(-50000, Currency.USD));

            Account updatedDestination = accountRepository.findById(destination.getId()).orElseThrow();
            assertThat(updatedDestination.getTotalBalance()).isEqualTo(new Money(50000, Currency.USD));
            assertThat(updatedDestination.getAvailableBalance()).isEqualTo(new Money(50000, Currency.USD));

            assertZeroSum();
        }

        @Test
        void shouldProcessReverseTransaction() {
            Account systemAccount = createAccount(AccountType.SYSTEM, Currency.USD, true);
            Account userAccount = createAccount(AccountType.USER, Currency.USD, false);
            fundAccount(systemAccount, userAccount, new Money(100000, Currency.USD));

            ProcessTransactionRequest request = new ProcessTransactionRequest(
                    "withdrawal-001",
                    userAccount.getId(),
                    systemAccount.getId(),
                    new Money(30000, Currency.USD),
                    "Withdrawal"
            );

            Transaction transaction = transactionProcessor.processAtomically(request);

            assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.COMPLETED);

            Account updatedUser = accountRepository.findById(userAccount.getId()).orElseThrow();
            assertThat(updatedUser.getTotalBalance()).isEqualTo(new Money(70000, Currency.USD));
            assertThat(updatedUser.getAvailableBalance()).isEqualTo(new Money(70000, Currency.USD));

            Account updatedSystem = accountRepository.findById(systemAccount.getId()).orElseThrow();
            assertThat(updatedSystem.getTotalBalance()).isEqualTo(new Money(-70000, Currency.USD));
            assertThat(updatedSystem.getAvailableBalance()).isEqualTo(new Money(-70000, Currency.USD));

            assertZeroSum();
        }

        @Test
        void shouldThrowForInsufficientFunds() {
            Account systemAccount = createAccount(AccountType.SYSTEM, Currency.USD, true);
            Account source = createAccount(AccountType.USER, Currency.USD, false);
            fundAccount(systemAccount, source, new Money(10000, Currency.USD));

            ProcessTransactionRequest request = new ProcessTransactionRequest(
                    "insufficient-001",
                    source.getId(),
                    systemAccount.getId(),
                    new Money(50000, Currency.USD),
                    "Should fail"
            );

            assertThatThrownBy(() -> transactionProcessor.processAtomically(request))
                    .isInstanceOf(InsufficientFundsException.class)
                    .hasMessage("Insufficient funds in account %s".formatted(source.getId()));

            assertThat(transactionRepository.findByIdempotencyKey("insufficient-001")).isEmpty();

            Account unchangedSource = accountRepository.findById(source.getId()).orElseThrow();
            assertThat(unchangedSource.getTotalBalance()).isEqualTo(new Money(10000, Currency.USD));
            assertThat(unchangedSource.getAvailableBalance()).isEqualTo(new Money(10000, Currency.USD));
        }

        @Test
        void shouldAllowOverdraftWhenEnabled() {
            Account source = createAccount(AccountType.USER, Currency.USD, true);
            Account destination = createAccount(AccountType.SYSTEM, Currency.USD, true);

            ProcessTransactionRequest request = new ProcessTransactionRequest(
                    "overdraft-001",
                    source.getId(),
                    destination.getId(),
                    new Money(50000, Currency.USD),
                    "Overdraft transaction"
            );

            Transaction transaction = transactionProcessor.processAtomically(request);

            assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.COMPLETED);

            Account updatedSource = accountRepository.findById(source.getId()).orElseThrow();
            assertThat(updatedSource.getTotalBalance()).isEqualTo(new Money(-50000, Currency.USD));
            assertThat(updatedSource.getAvailableBalance()).isEqualTo(new Money(-50000, Currency.USD));

            assertZeroSum();
        }

        @Test
        void shouldThrowForCurrencyMismatch() {
            Account source = createAccount(AccountType.USER, Currency.USD, true);
            Account destination = createAccount(AccountType.USER, Currency.EUR, false);

            ProcessTransactionRequest request = new ProcessTransactionRequest(
                    "currency-001",
                    source.getId(),
                    destination.getId(),
                    new Money(10000, Currency.USD),
                    "Currency mismatch"
            );

            assertThatThrownBy(() -> transactionProcessor.processAtomically(request))
                    .isInstanceOf(CurrencyMismatchException.class)
                    .hasMessage("Source account currency USD does not match destination account currency EUR");

            assertThat(transactionRepository.findByIdempotencyKey("currency-001")).isEmpty();
        }

        @Test
        void shouldReturnExistingTransactionForDuplicateIdempotencyKey() {
            Account source = createAccount(AccountType.SYSTEM, Currency.USD, true);
            Account destination = createAccount(AccountType.USER, Currency.USD, false);

            ProcessTransactionRequest request = new ProcessTransactionRequest(
                    "idempotent-001",
                    source.getId(),
                    destination.getId(),
                    new Money(50000, Currency.USD),
                    "First request"
            );

            Transaction firstTransaction = transactionProcessor.processAtomically(request);
            Transaction secondTransaction = transactionProcessor.processAtomically(request);

            assertEqualsIgnoringFields(secondTransaction, firstTransaction, "isNew");

            Account updatedDestination = accountRepository.findById(destination.getId()).orElseThrow();
            assertThat(updatedDestination.getTotalBalance()).isEqualTo(new Money(50000, Currency.USD));
        }

        @Test
        void shouldMaintainZeroSumInvariant() {
            Account systemAccount = createAccount(AccountType.SYSTEM, Currency.USD, true);
            Account userAccount = createAccount(AccountType.USER, Currency.USD, false);

            transactionProcessor.processAtomically(new ProcessTransactionRequest(
                    "deposit-zs", systemAccount.getId(), userAccount.getId(),
                    new Money(100000, Currency.USD), "Deposit"
            ));
            assertZeroSum();

            transactionProcessor.processAtomically(new ProcessTransactionRequest(
                    "withdrawal-zs", userAccount.getId(), systemAccount.getId(),
                    new Money(30000, Currency.USD), "Withdrawal"
            ));
            assertZeroSum();
        }

        @Test
        void shouldThrowWhenAccountNotFound() {
            Account source = createAccount(AccountType.USER, Currency.USD, false);
            UUID nonExistentId = UUID.randomUUID();

            ProcessTransactionRequest request = new ProcessTransactionRequest(
                    "not-found-001",
                    source.getId(),
                    nonExistentId,
                    new Money(10000, Currency.USD),
                    "Should fail"
            );

            assertThatThrownBy(() -> transactionProcessor.processAtomically(request))
                    .isInstanceOf(AccountNotFoundException.class)
                    .hasMessage("Account with id %s not found".formatted(nonExistentId));
        }
    }

    @Nested
    class Initiate {

        @Test
        void shouldInitiateTransaction() {
            Account source = createAccount(AccountType.SYSTEM, Currency.USD, true);
            Account destination = createAccount(AccountType.USER, Currency.USD, false);

            ProcessTransactionRequest request = new ProcessTransactionRequest(
                    "initiate-001",
                    source.getId(),
                    destination.getId(),
                    new Money(50000, Currency.USD),
                    "Pending deposit"
            );

            Transaction transaction = transactionProcessor.initiate(request);

            Transaction expected = Transaction.builder()
                    .id(transaction.getId())
                    .status(TransactionStatus.PENDING)
                    .sourceAccountId(source.getId())
                    .destinationAccountId(destination.getId())
                    .amount(new Money(50000, Currency.USD))
                    .idempotencyKey("initiate-001")
                    .description("Pending deposit")
                    .createdAt(FIXED_INSTANT)
                    .updatedAt(FIXED_INSTANT)
                    .build();
            assertEqualsIgnoringFields(transaction, expected, "isNew");

            Account updatedSource = accountRepository.findById(source.getId()).orElseThrow();
            assertThat(updatedSource.getTotalBalance()).isEqualTo(new Money(0, Currency.USD));
            assertThat(updatedSource.getAvailableBalance()).isEqualTo(new Money(-50000, Currency.USD));

            Account unchangedDestination = accountRepository.findById(destination.getId()).orElseThrow();
            assertThat(unchangedDestination.getTotalBalance()).isEqualTo(new Money(0, Currency.USD));
            assertThat(unchangedDestination.getAvailableBalance()).isEqualTo(new Money(0, Currency.USD));
        }

        @Test
        void shouldAllowOverdraftWhenEnabled() {
            Account source = createAccount(AccountType.USER, Currency.USD, true);
            Account destination = createAccount(AccountType.SYSTEM, Currency.USD, true);

            ProcessTransactionRequest request = new ProcessTransactionRequest(
                    "initiate-overdraft-001",
                    source.getId(),
                    destination.getId(),
                    new Money(50000, Currency.USD),
                    "Overdraft initiate"
            );

            Transaction transaction = transactionProcessor.initiate(request);

            assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.PENDING);

            Account updatedSource = accountRepository.findById(source.getId()).orElseThrow();
            assertThat(updatedSource.getTotalBalance()).isEqualTo(new Money(0, Currency.USD));
            assertThat(updatedSource.getAvailableBalance()).isEqualTo(new Money(-50000, Currency.USD));
        }

        @Test
        void shouldThrowForInsufficientFunds() {
            Account systemAccount = createAccount(AccountType.SYSTEM, Currency.USD, true);
            Account source = createAccount(AccountType.USER, Currency.USD, false);
            fundAccount(systemAccount, source, new Money(10000, Currency.USD));

            ProcessTransactionRequest request = new ProcessTransactionRequest(
                    "initiate-insufficient-001",
                    source.getId(),
                    systemAccount.getId(),
                    new Money(50000, Currency.USD),
                    "Should fail"
            );

            assertThatThrownBy(() -> transactionProcessor.initiate(request))
                    .isInstanceOf(InsufficientFundsException.class)
                    .hasMessage("Insufficient funds in account %s".formatted(source.getId()));

            assertThat(transactionRepository.findByIdempotencyKey("initiate-insufficient-001")).isEmpty();
        }

        @Test
        void shouldThrowWhenSourceNotFound() {
            UUID nonExistentId = UUID.randomUUID();
            Account destination = createAccount(AccountType.USER, Currency.USD, false);

            ProcessTransactionRequest request = new ProcessTransactionRequest(
                    "initiate-source-not-found-001",
                    nonExistentId,
                    destination.getId(),
                    new Money(10000, Currency.USD),
                    "Should fail"
            );

            assertThatThrownBy(() -> transactionProcessor.initiate(request))
                    .isInstanceOf(AccountNotFoundException.class)
                    .hasMessage("Account with id %s not found".formatted(nonExistentId));

            assertThat(transactionRepository.findByIdempotencyKey("initiate-source-not-found-001")).isEmpty();
        }

        @Test
        void shouldThrowForSourceCurrencyMismatch() {
            Account source = createAccount(AccountType.USER, Currency.EUR, true);
            Account destination = createAccount(AccountType.USER, Currency.USD, false);

            ProcessTransactionRequest request = new ProcessTransactionRequest(
                    "initiate-currency-001",
                    source.getId(),
                    destination.getId(),
                    new Money(10000, Currency.USD),
                    "Currency mismatch"
            );

            assertThatThrownBy(() -> transactionProcessor.initiate(request))
                    .isInstanceOf(CurrencyMismatchException.class)
                    .hasMessage("Account currency EUR does not match transaction currency USD");

            assertThat(transactionRepository.findByIdempotencyKey("initiate-currency-001")).isEmpty();
        }

        @Test
        void shouldThrowForDestinationCurrencyMismatch() {
            Account source = createAccount(AccountType.USER, Currency.USD, true);
            Account destination = createAccount(AccountType.USER, Currency.EUR, false);

            ProcessTransactionRequest request = new ProcessTransactionRequest(
                    "initiate-dest-currency-001",
                    source.getId(),
                    destination.getId(),
                    new Money(10000, Currency.USD),
                    "Destination currency mismatch"
            );

            assertThatThrownBy(() -> transactionProcessor.initiate(request))
                    .isInstanceOf(CurrencyMismatchException.class)
                    .hasMessage("Source account currency USD does not match destination account currency EUR");

            assertThat(transactionRepository.findByIdempotencyKey("initiate-dest-currency-001")).isEmpty();
        }

        @Test
        void shouldThrowWhenDestinationNotFound() {
            Account source = createAccount(AccountType.USER, Currency.USD, true);
            UUID nonExistentId = UUID.randomUUID();

            ProcessTransactionRequest request = new ProcessTransactionRequest(
                    "initiate-not-found-001",
                    source.getId(),
                    nonExistentId,
                    new Money(10000, Currency.USD),
                    "Should fail"
            );

            assertThatThrownBy(() -> transactionProcessor.initiate(request))
                    .isInstanceOf(AccountNotFoundException.class)
                    .hasMessage("Account with id %s not found".formatted(nonExistentId));

            assertThat(transactionRepository.findByIdempotencyKey("initiate-not-found-001")).isEmpty();
        }

        @Test
        void shouldReturnExistingTransactionForDuplicateIdempotencyKey() {
            Account source = createAccount(AccountType.SYSTEM, Currency.USD, true);
            Account destination = createAccount(AccountType.USER, Currency.USD, false);

            ProcessTransactionRequest request = new ProcessTransactionRequest(
                    "initiate-idempotent-001",
                    source.getId(),
                    destination.getId(),
                    new Money(50000, Currency.USD),
                    "First request"
            );

            Transaction firstTransaction = transactionProcessor.initiate(request);
            Transaction secondTransaction = transactionProcessor.initiate(request);

            assertEqualsIgnoringFields(secondTransaction, firstTransaction, "isNew");

            Account updatedSource = accountRepository.findById(source.getId()).orElseThrow();
            assertThat(updatedSource.getAvailableBalance()).isEqualTo(new Money(-50000, Currency.USD));
        }
    }

    @Nested
    class Complete {

        @Test
        void shouldCompleteInitiatedTransaction() {
            Account source = createAccount(AccountType.SYSTEM, Currency.USD, true);
            Account destination = createAccount(AccountType.USER, Currency.USD, false);

            Transaction pending = transactionProcessor.initiate(new ProcessTransactionRequest(
                    "complete-001", source.getId(), destination.getId(),
                    new Money(50000, Currency.USD), "Deposit"
            ));

            Transaction completed = transactionProcessor.complete(pending.getId());

            assertThat(completed.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
            assertThat(completed.getDeclineReason()).isNull();

            Account updatedSource = accountRepository.findById(source.getId()).orElseThrow();
            assertThat(updatedSource.getTotalBalance()).isEqualTo(new Money(-50000, Currency.USD));
            assertThat(updatedSource.getAvailableBalance()).isEqualTo(new Money(-50000, Currency.USD));

            Account updatedDestination = accountRepository.findById(destination.getId()).orElseThrow();
            assertThat(updatedDestination.getTotalBalance()).isEqualTo(new Money(50000, Currency.USD));
            assertThat(updatedDestination.getAvailableBalance()).isEqualTo(new Money(50000, Currency.USD));

            assertZeroSum();
        }

        @Test
        void shouldThrowForNonExistentTransaction() {
            UUID nonExistentId = UUID.randomUUID();

            assertThatThrownBy(() -> transactionProcessor.complete(nonExistentId))
                    .isInstanceOf(TransactionNotFoundException.class)
                    .hasMessage("Transaction with id %s not found".formatted(nonExistentId));
        }

        @Test
        void shouldThrowForNonPendingTransaction() {
            Account source = createAccount(AccountType.SYSTEM, Currency.USD, true);
            Account destination = createAccount(AccountType.USER, Currency.USD, false);

            Transaction pending = transactionProcessor.initiate(new ProcessTransactionRequest(
                    "non-pending-001", source.getId(), destination.getId(),
                    new Money(50000, Currency.USD), "Deposit"
            ));

            transactionProcessor.complete(pending.getId());

            assertThatThrownBy(() -> transactionProcessor.complete(pending.getId()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Transaction %s is not in PENDING status, current status: COMPLETED".formatted(pending.getId()));
        }

        @Test
        void shouldMaintainZeroSumAfterTwoPhaseFlow() {
            Account systemAccount = createAccount(AccountType.SYSTEM, Currency.USD, true);
            Account userAccount = createAccount(AccountType.USER, Currency.USD, false);

            Transaction deposit = transactionProcessor.initiate(new ProcessTransactionRequest(
                    "two-phase-zs-1", systemAccount.getId(), userAccount.getId(),
                    new Money(100000, Currency.USD), "Deposit"
            ));
            transactionProcessor.complete(deposit.getId());
            assertZeroSum();

            Transaction withdrawal = transactionProcessor.initiate(new ProcessTransactionRequest(
                    "two-phase-zs-2", userAccount.getId(), systemAccount.getId(),
                    new Money(30000, Currency.USD), "Withdrawal"
            ));
            transactionProcessor.complete(withdrawal.getId());
            assertZeroSum();
        }
    }

    @Nested
    class Decline {

        @Test
        void shouldDeclineInitiatedTransaction() {
            Account systemAccount = createAccount(AccountType.SYSTEM, Currency.USD, true);
            Account source = createAccount(AccountType.USER, Currency.USD, false);
            fundAccount(systemAccount, source, new Money(100000, Currency.USD));
            Account destination = createAccount(AccountType.SYSTEM, Currency.USD, true);

            Transaction pending = transactionProcessor.initiate(new ProcessTransactionRequest(
                    "decline-001", source.getId(), destination.getId(),
                    new Money(30000, Currency.USD), "Withdrawal"
            ));

            Account sourceAfterHold = accountRepository.findById(source.getId()).orElseThrow();
            assertThat(sourceAfterHold.getTotalBalance()).isEqualTo(new Money(100000, Currency.USD));
            assertThat(sourceAfterHold.getAvailableBalance()).isEqualTo(new Money(70000, Currency.USD));

            Transaction declined = transactionProcessor.decline(pending.getId(), "User cancelled");

            assertThat(declined.getStatus()).isEqualTo(TransactionStatus.DECLINED);
            assertThat(declined.getDeclineReason()).isEqualTo("User cancelled");

            Account sourceAfterDecline = accountRepository.findById(source.getId()).orElseThrow();
            assertThat(sourceAfterDecline.getTotalBalance()).isEqualTo(new Money(100000, Currency.USD));
            assertThat(sourceAfterDecline.getAvailableBalance()).isEqualTo(new Money(100000, Currency.USD));

            Account unchangedDestination = accountRepository.findById(destination.getId()).orElseThrow();
            assertThat(unchangedDestination.getTotalBalance()).isEqualTo(new Money(0, Currency.USD));
            assertThat(unchangedDestination.getAvailableBalance()).isEqualTo(new Money(0, Currency.USD));
        }

        @Test
        void shouldThrowForNonExistentTransaction() {
            UUID nonExistentId = UUID.randomUUID();

            assertThatThrownBy(() -> transactionProcessor.decline(nonExistentId, "reason"))
                    .isInstanceOf(TransactionNotFoundException.class)
                    .hasMessage("Transaction with id %s not found".formatted(nonExistentId));
        }

        @Test
        void shouldThrowForNonPendingTransaction() {
            Account source = createAccount(AccountType.SYSTEM, Currency.USD, true);
            Account destination = createAccount(AccountType.USER, Currency.USD, false);

            Transaction pending = transactionProcessor.initiate(new ProcessTransactionRequest(
                    "decline-non-pending-001", source.getId(), destination.getId(),
                    new Money(50000, Currency.USD), "Deposit"
            ));

            transactionProcessor.complete(pending.getId());

            assertThatThrownBy(() -> transactionProcessor.decline(pending.getId(), "Too late"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Transaction %s is not in PENDING status, current status: COMPLETED".formatted(pending.getId()));
        }

        @Test
        void shouldThrowForNullReason() {
            assertThatThrownBy(() -> transactionProcessor.decline(UUID.randomUUID(), null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("Decline reason must not be null");
        }

        @Test
        void shouldThrowForBlankReason() {
            assertThatThrownBy(() -> transactionProcessor.decline(UUID.randomUUID(), "  "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Decline reason must not be blank");
        }
    }

    private Account createAccount(AccountType type, Currency currency, boolean overdraftEnabled) {
        Instant now = clock.now();
        Account account = Account.builder()
                .id(UUID.randomUUID())
                .accountNumber("ACC-" + UUID.randomUUID().toString().substring(0, 8))
                .userId(UUID.randomUUID())
                .type(type)
                .currency(currency)
                .overdraftEnabled(overdraftEnabled)
                .createdAt(now)
                .updatedAt(now)
                .build();
        accountRepository.save(account);
        return account;
    }

    private void fundAccount(Account systemAccount, Account userAccount, Money amount) {
        transactionProcessor.processAtomically(new ProcessTransactionRequest(
                "fund-" + idempotencyCounter.incrementAndGet(),
                systemAccount.getId(),
                userAccount.getId(),
                amount,
                "Test funding"
        ));
    }

    private void assertZeroSum() {
        long sum = 0;
        for (Account account : accountRepository.findAll()) {
            sum += account.getTotalBalance().amount();
        }
        assertThat(sum).isZero();
    }
}
