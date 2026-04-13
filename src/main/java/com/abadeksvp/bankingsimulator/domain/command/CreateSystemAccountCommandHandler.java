package com.abadeksvp.bankingsimulator.domain.command;

import com.abadeksvp.bankingsimulator.common.Clock;
import com.abadeksvp.bankingsimulator.cqrs.command.AbstractCommandHandler;
import com.abadeksvp.bankingsimulator.cqrs.core.AppError;
import com.abadeksvp.bankingsimulator.cqrs.core.Result;
import com.abadeksvp.bankingsimulator.domain.error.AccountErrorCode;
import com.abadeksvp.bankingsimulator.domain.model.Account;
import com.abadeksvp.bankingsimulator.domain.model.AccountType;
import com.abadeksvp.bankingsimulator.domain.repository.AccountRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class CreateSystemAccountCommandHandler extends AbstractCommandHandler<CreateSystemAccountCommand, UUID> {

    public static final UUID SYSTEM_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    private final AccountRepository accountRepository;
    private final Clock clock;

    public CreateSystemAccountCommandHandler(AccountRepository accountRepository, Clock clock) {
        super(CreateSystemAccountCommand.class);
        this.accountRepository = accountRepository;
        this.clock = clock;
    }

    @Override
    public Result<UUID> handle(CreateSystemAccountCommand command) {
        String accountNumber = "SYSTEM-" + command.currency().name();

        if (accountRepository.findByAccountNumber(accountNumber).isPresent()) {
            return Result.failure(new AppError(
                    AccountErrorCode.ACCOUNT_ALREADY_EXISTS,
                    "Account with number %s already exists".formatted(accountNumber)
            ));
        }

        Instant now = clock.now();
        UUID accountId = UUID.randomUUID();

        Account account = Account.builder()
                .id(accountId)
                .accountNumber(accountNumber)
                .userId(SYSTEM_USER_ID)
                .type(AccountType.SYSTEM)
                .currency(command.currency())
                .overdraftEnabled(true)
                .createdAt(now)
                .updatedAt(now)
                .build();

        accountRepository.save(account);

        return Result.success(accountId);
    }
}
