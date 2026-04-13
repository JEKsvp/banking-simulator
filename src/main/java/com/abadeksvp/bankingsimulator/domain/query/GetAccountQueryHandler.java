package com.abadeksvp.bankingsimulator.domain.query;

import com.abadeksvp.bankingsimulator.cqrs.core.AppError;
import com.abadeksvp.bankingsimulator.cqrs.core.Result;
import com.abadeksvp.bankingsimulator.cqrs.query.AbstractQueryHandler;
import com.abadeksvp.bankingsimulator.domain.error.AccountErrorCode;
import com.abadeksvp.bankingsimulator.domain.repository.AccountRepository;
import org.springframework.stereotype.Component;

@Component
public class GetAccountQueryHandler extends AbstractQueryHandler<GetAccountQuery, AccountView> {

    private final AccountRepository accountRepository;

    public GetAccountQueryHandler(AccountRepository accountRepository) {
        super(GetAccountQuery.class);
        this.accountRepository = accountRepository;
    }

    @Override
    public Result<AccountView> handle(GetAccountQuery query) {
        return accountRepository.findById(query.accountId())
                .map(account -> Result.success(AccountView.from(account)))
                .orElseGet(() -> Result.failure(new AppError(
                        AccountErrorCode.ACCOUNT_NOT_FOUND,
                        "Account with id %s not found".formatted(query.accountId())
                )));
    }
}
