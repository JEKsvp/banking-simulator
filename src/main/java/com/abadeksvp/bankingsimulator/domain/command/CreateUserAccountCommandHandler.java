package com.abadeksvp.bankingsimulator.domain.command;

import com.abadeksvp.bankingsimulator.common.Clock;
import com.abadeksvp.bankingsimulator.cqrs.command.AbstractCommandHandler;
import com.abadeksvp.bankingsimulator.cqrs.core.AppError;
import com.abadeksvp.bankingsimulator.cqrs.core.Result;
import com.abadeksvp.bankingsimulator.domain.error.TransactionErrorCode;
import com.abadeksvp.bankingsimulator.domain.model.Account;
import com.abadeksvp.bankingsimulator.domain.model.AccountType;
import com.abadeksvp.bankingsimulator.domain.repository.AccountRepository;
import com.abadeksvp.bankingsimulator.domain.service.ProcessTransactionRequest;
import com.abadeksvp.bankingsimulator.domain.service.TransactionProcessor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Component
public class CreateUserAccountCommandHandler extends AbstractCommandHandler<CreateUserAccountCommand, UUID> {

    private final AccountRepository accountRepository;
    private final TransactionProcessor transactionProcessor;
    private final Clock clock;

    public CreateUserAccountCommandHandler(AccountRepository accountRepository,
                                           TransactionProcessor transactionProcessor,
                                           Clock clock) {
        super(CreateUserAccountCommand.class);
        this.accountRepository = accountRepository;
        this.transactionProcessor = transactionProcessor;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Result<UUID> handle(CreateUserAccountCommand command) {
        Account systemAccount = null;
        if (command.initialDeposit() != null) {
            systemAccount = accountRepository
                    .findByTypeAndCurrency(AccountType.SYSTEM, command.currency())
                    .orElse(null);

            if (systemAccount == null) {
                return Result.failure(new AppError(
                        TransactionErrorCode.SYSTEM_ACCOUNT_NOT_FOUND,
                        "No system account found for currency %s".formatted(command.currency())
                ));
            }
        }

        Instant now = clock.now();
        UUID accountId = UUID.randomUUID();
        String accountNumber = "ACC-" + accountId;

        Account account = Account.builder()
                .id(accountId)
                .accountNumber(accountNumber)
                .userId(command.userId())
                .type(AccountType.USER)
                .currency(command.currency())
                .overdraftEnabled(command.overdraftEnabled())
                .createdAt(now)
                .updatedAt(now)
                .build();

        accountRepository.save(account);

        if (command.initialDeposit() != null) {
            ProcessTransactionRequest depositRequest = new ProcessTransactionRequest(
                    "initial-deposit-" + accountId,
                    systemAccount.getId(),
                    accountId,
                    command.initialDeposit(),
                    "Initial deposit"
            );

            transactionProcessor.processAtomically(depositRequest);
        }

        return Result.success(accountId);
    }
}
