package com.abadeksvp.bankingsimulator.cqrs.query;

public abstract class AbstractQueryHandler<Q extends Query<R>, R> implements QueryHandler<Q, R> {

    private final Class<Q> queryType;

    protected AbstractQueryHandler(Class<Q> queryType) {
        this.queryType = queryType;
    }

    @Override
    public Class<Q> getQueryType() {
        return queryType;
    }
}
