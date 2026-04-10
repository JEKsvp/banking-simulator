package com.abadeksvp.bankingsimulator.domain.query;

import com.abadeksvp.bankingsimulator.cqrs.query.Query;
import com.abadeksvp.bankingsimulator.domain.model.Account;

import java.util.UUID;

public record GetAccountQuery(UUID accountId) implements Query<Account> {
}
