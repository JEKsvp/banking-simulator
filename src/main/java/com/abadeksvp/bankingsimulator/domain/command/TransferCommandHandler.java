package com.abadeksvp.bankingsimulator.domain.command;

import com.abadeksvp.bankingsimulator.cqrs.command.AbstractCommandHandler;
import com.abadeksvp.bankingsimulator.cqrs.core.AppError;
import com.abadeksvp.bankingsimulator.cqrs.core.Result;
import com.abadeksvp.bankingsimulator.domain.error.AccountErrorCode;
import com.abadeksvp.bankingsimulator.domain.error.AccountNotFoundException;
import com.abadeksvp.bankingsimulator.domain.error.CurrencyMismatchException;
import com.abadeksvp.bankingsimulator.domain.error.InsufficientFundsException;
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
public class TransferCommandHandler extends AbstractCommandHandler<TransferCommand, UUID> {

    private final AccountRepository accountRepository;
    private final TransactionProcessor transactionProcessor;

    public TransferCommandHandler(AccountRepository accountRepository,
                                  TransactionProcessor transactionProcessor) {
        super(TransferCommand.class);
        this.accountRepository = accountRepository;
        this.transactionProcessor = transactionProcessor;
    }

    @Override
    public Result<UUID> handle(TransferCommand command) {
        Account source = accountRepository.findById(command.sourceAccountId()).orElse(null);
        if (source == null) {
            return Result.failure(new AppError(AccountErrorCode.ACCOUNT_NOT_FOUND,
                    "Account with id %s not found".formatted(command.sourceAccountId())));
        }
        if (source.getType() != AccountType.USER) {
            return Result.failure(new AppError(TransactionErrorCode.TRANSFER_NOT_ALLOWED,
                    "Transfers are only allowed between user accounts"));
        }

        Account destination = accountRepository.findById(command.destinationAccountId()).orElse(null);
        if (destination == null) {
            return Result.failure(new AppError(AccountErrorCode.ACCOUNT_NOT_FOUND,
                    "Account with id %s not found".formatted(command.destinationAccountId())));
        }
        if (destination.getType() != AccountType.USER) {
            return Result.failure(new AppError(TransactionErrorCode.TRANSFER_NOT_ALLOWED,
                    "Transfers are only allowed between user accounts"));
        }

        try {
            ProcessTransactionRequest request = new ProcessTransactionRequest(
                    command.idempotencyKey(),
                    command.sourceAccountId(),
                    command.destinationAccountId(),
                    command.amount(),
                    command.description()
            );

            Transaction transaction = transactionProcessor.processAtomically(request);
            return Result.success(transaction.getId());
        } catch (AccountNotFoundException e) {
            return Result.failure(new AppError(AccountErrorCode.ACCOUNT_NOT_FOUND, e.getMessage()));
        } catch (InsufficientFundsException e) {
            return Result.failure(new AppError(TransactionErrorCode.INSUFFICIENT_FUNDS, e.getMessage()));
        } catch (CurrencyMismatchException e) {
            return Result.failure(new AppError(TransactionErrorCode.CURRENCY_MISMATCH, e.getMessage()));
        }
    }
}
