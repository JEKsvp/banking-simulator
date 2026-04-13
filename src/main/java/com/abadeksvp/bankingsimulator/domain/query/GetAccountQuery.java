package com.abadeksvp.bankingsimulator.domain.query;

import com.abadeksvp.bankingsimulator.cqrs.query.Query;

import java.util.UUID;

public record GetAccountQuery(UUID accountId) implements Query<AccountView> {
}
