package com.abadeksvp.bankingsimulator.domain.service;

import com.abadeksvp.bankingsimulator.common.Clock;
import com.abadeksvp.bankingsimulator.domain.error.AccountNotFoundException;
import com.abadeksvp.bankingsimulator.domain.error.CurrencyMismatchException;
import com.abadeksvp.bankingsimulator.domain.error.InsufficientFundsException;
import com.abadeksvp.bankingsimulator.domain.error.TransactionNotFoundException;
import com.abadeksvp.bankingsimulator.domain.model.Account;
import com.abadeksvp.bankingsimulator.domain.model.Money;
import com.abadeksvp.bankingsimulator.domain.model.Transaction;
import com.abadeksvp.bankingsimulator.domain.model.TransactionStatus;
import com.abadeksvp.bankingsimulator.domain.repository.AccountRepository;
import com.abadeksvp.bankingsimulator.domain.repository.TransactionRepository;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionProcessor {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final AccountLocker accountLocker;
    private final Clock clock;

    public TransactionProcessor(AccountRepository accountRepository,
                                TransactionRepository transactionRepository,
                                AccountLocker accountLocker,
                                Clock clock) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.accountLocker = accountLocker;
        this.clock = clock;
    }

    @Transactional
    public Transaction processAtomically(ProcessTransactionRequest request) {
        Optional<Transaction> existing = transactionRepository.findByIdempotencyKey(request.idempotencyKey());
        if (existing.isPresent()) {
            return existing.get();
        }

        AccountLocker.AccountPair accounts =
                accountLocker.lockForTransaction(request.sourceAccountId(), request.destinationAccountId());

        Account source = accounts.source();
        Account destination = accounts.destination();

        validateAccountCurrenciesMatch(source, destination);

        Money amount = request.amount();

        if (!source.hasSufficientBalance(amount)) {
            throw new InsufficientFundsException(request.sourceAccountId());
        }

        Instant now = clock.now();

        source.hold(amount, now);
        source.debit(amount, now);
        destination.credit(amount, now);

        accountRepository.save(source);
        accountRepository.save(destination);

        Transaction transaction = createTransaction(request, TransactionStatus.COMPLETED, now);
        transactionRepository.save(transaction);

        return transaction;
    }

    @Transactional
    public Transaction initiate(ProcessTransactionRequest request) {
        Optional<Transaction> existing = transactionRepository.findByIdempotencyKey(request.idempotencyKey());
        if (existing.isPresent()) {
            return existing.get();
        }

        Account source = accountLocker.lockAccount(request.sourceAccountId());

        Money amount = request.amount();

        if (!source.hasSufficientBalance(amount)) {
            throw new InsufficientFundsException(request.sourceAccountId());
        }

        Account destination = accountRepository.findById(request.destinationAccountId())
                .orElseThrow(() -> new AccountNotFoundException(request.destinationAccountId()));

        validateAccountCurrenciesMatch(source, destination);

        Instant now = clock.now();

        source.hold(amount, now);
        accountRepository.save(source);

        Transaction transaction = createTransaction(request, TransactionStatus.PENDING, now);
        transactionRepository.save(transaction);

        return transaction;
    }

    @Transactional
    public Transaction complete(UUID transactionId) {
        Objects.requireNonNull(transactionId, "Transaction ID must not be null");

        Transaction transaction = findPendingForUpdate(transactionId);

        AccountLocker.AccountPair accounts = accountLocker.lockForTransaction(
                transaction.getSourceAccountId(), transaction.getDestinationAccountId());

        Account source = accounts.source();
        Account destination = accounts.destination();

        Instant now = clock.now();

        if (source.getCurrency() != transaction.getAmount().currency()) {
            return declineTransaction(transaction, source, now,
                    "Currency mismatch: source account %s vs transaction %s"
                            .formatted(source.getCurrency(), transaction.getAmount().currency()));
        }
        if (destination.getCurrency() != transaction.getAmount().currency()) {
            return declineTransaction(transaction, source, now,
                    "Currency mismatch: destination account %s vs transaction %s"
                            .formatted(destination.getCurrency(), transaction.getAmount().currency()));
        }

        Money amount = transaction.getAmount();

        source.debit(amount, now);
        destination.credit(amount, now);

        transaction.transition(TransactionStatus.COMPLETED, now);

        accountRepository.save(source);
        accountRepository.save(destination);
        transactionRepository.save(transaction);

        return transaction;
    }

    @Transactional
    public Transaction decline(UUID transactionId, String reason) {
        Objects.requireNonNull(transactionId, "Transaction ID must not be null");
        Objects.requireNonNull(reason, "Decline reason must not be null");
        if (reason.isBlank()) {
            throw new IllegalArgumentException("Decline reason must not be blank");
        }

        Transaction transaction = findPendingForUpdate(transactionId);
        Account source = accountLocker.lockAccount(transaction.getSourceAccountId());
        Instant now = clock.now();

        return declineTransaction(transaction, source, now, reason);
    }

    private Transaction findPendingForUpdate(UUID transactionId) {
        Transaction transaction = transactionRepository.findByIdForUpdate(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId));

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new IllegalStateException(
                    "Transaction %s is not in PENDING status, current status: %s"
                            .formatted(transactionId, transaction.getStatus()));
        }

        return transaction;
    }

    private void validateAccountCurrenciesMatch(Account source, Account destination) {
        if (source.getCurrency() != destination.getCurrency()) {
            throw CurrencyMismatchException.betweenAccounts(source.getCurrency(), destination.getCurrency());
        }
    }

    private Transaction createTransaction(ProcessTransactionRequest request, TransactionStatus status, Instant now) {
        return Transaction.builder()
                .id(UUID.randomUUID())
                .status(status)
                .sourceAccountId(request.sourceAccountId())
                .destinationAccountId(request.destinationAccountId())
                .amount(request.amount())
                .idempotencyKey(request.idempotencyKey())
                .description(request.description())
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private Transaction declineTransaction(Transaction transaction, Account source, Instant now, String reason) {
        Money holdAmount = new Money(transaction.getAmount().amount(), source.getCurrency());
        source.releaseHold(holdAmount, now);
        accountRepository.save(source);

        transaction.decline(reason, now);
        transactionRepository.save(transaction);

        return transaction;
    }
}
