package com.abadeksvp.bankingsimulator.cqrs.query;

import com.abadeksvp.bankingsimulator.cqrs.core.Result;

public interface QueryHandler<Q extends Query<R>, R> {

    Class<Q> getQueryType();

    Result<R> handle(Q query);
}
