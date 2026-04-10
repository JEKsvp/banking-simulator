package com.abadeksvp.bankingsimulator.domain.command;

import com.abadeksvp.bankingsimulator.common.Clock;
import com.abadeksvp.bankingsimulator.cqrs.command.AbstractCommandHandler;
import com.abadeksvp.bankingsimulator.cqrs.core.AppError;
import com.abadeksvp.bankingsimulator.cqrs.core.Result;
import com.abadeksvp.bankingsimulator.domain.error.AccountErrorCode;
import com.abadeksvp.bankingsimulator.domain.model.Account;
import com.abadeksvp.bankingsimulator.domain.repository.AccountRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class CreateAccountCommandHandler extends AbstractCommandHandler<CreateAccountCommand, UUID> {

    private final AccountRepository accountRepository;
    private final Clock clock;

    public CreateAccountCommandHandler(AccountRepository accountRepository, Clock clock) {
        super(CreateAccountCommand.class);
        this.accountRepository = accountRepository;
        this.clock = clock;
    }

    @Override
    public Result<UUID> handle(CreateAccountCommand command) {
        if (accountRepository.findByAccountNumber(command.accountNumber()).isPresent()) {
            return Result.failure(new AppError(
                    AccountErrorCode.ACCOUNT_ALREADY_EXISTS,
                    "Account with number %s already exists".formatted(command.accountNumber())
            ));
        }

        Instant now = clock.now();
        UUID accountId = UUID.randomUUID();

        Account account = Account.builder()
                .id(accountId)
                .accountNumber(command.accountNumber())
                .userId(command.userId())
                .type(command.accountType())
                .balance(command.initialDeposit())
                .overdraftEnabled(command.overdraftEnabled())
                .createdAt(now)
                .updatedAt(now)
                .build();

        accountRepository.save(account);

        return Result.success(accountId);
    }
}
