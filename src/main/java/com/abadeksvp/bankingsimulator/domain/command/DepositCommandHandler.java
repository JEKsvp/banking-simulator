package com.abadeksvp.bankingsimulator.domain.command;

import com.abadeksvp.bankingsimulator.cqrs.command.AbstractCommandHandler;
import com.abadeksvp.bankingsimulator.cqrs.core.AppError;
import com.abadeksvp.bankingsimulator.cqrs.core.Result;
import com.abadeksvp.bankingsimulator.domain.error.AccountErrorCode;
import com.abadeksvp.bankingsimulator.domain.error.CurrencyMismatchException;
import com.abadeksvp.bankingsimulator.domain.error.TransactionErrorCode;
import com.abadeksvp.bankingsimulator.domain.model.Account;
import com.abadeksvp.bankingsimulator.domain.model.AccountType;
import com.abadeksvp.bankingsimulator.domain.model.Transaction;
import com.abadeksvp.bankingsimulator.domain.repository.AccountRepository;
import com.abadeksvp.bankingsimulator.domain.service.ProcessTransactionRequest;
import com.abadeksvp.bankingsimulator.domain.service.TransactionProcessor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DepositCommandHandler extends AbstractCommandHandler<DepositCommand, UUID> {

    private final AccountRepository accountRepository;
    private final TransactionProcessor transactionProcessor;

    public DepositCommandHandler(AccountRepository accountRepository,
                                 TransactionProcessor transactionProcessor) {
        super(DepositCommand.class);
        this.accountRepository = accountRepository;
        this.transactionProcessor = transactionProcessor;
    }

    @Override
    public Result<UUID> handle(DepositCommand command) {
        Account targetAccount = accountRepository.findById(command.accountId()).orElse(null);
        if (targetAccount == null) {
            return Result.failure(new AppError(AccountErrorCode.ACCOUNT_NOT_FOUND,
                    "Account with id %s not found".formatted(command.accountId())));
        }
        if (targetAccount.getType() != AccountType.USER) {
            return Result.failure(new AppError(TransactionErrorCode.DEPOSIT_NOT_ALLOWED,
                    "Deposits are only allowed to user accounts"));
        }

        Account systemAccount = accountRepository
                .findByTypeAndCurrency(AccountType.SYSTEM, command.amount().currency())
                .orElse(null);

        if (systemAccount == null) {
            return Result.failure(new AppError(
                    TransactionErrorCode.SYSTEM_ACCOUNT_NOT_FOUND,
                    "No system account found for currency %s".formatted(command.amount().currency())
            ));
        }

        try {
            ProcessTransactionRequest request = new ProcessTransactionRequest(
                    command.idempotencyKey(),
                    systemAccount.getId(),
                    command.accountId(),
                    command.amount(),
                    command.description()
            );

            Transaction transaction = transactionProcessor.processAtomically(request);
            return Result.success(transaction.getId());
        } catch (CurrencyMismatchException e) {
            return Result.failure(new AppError(TransactionErrorCode.CURRENCY_MISMATCH, e.getMessage()));
        }
    }
}
