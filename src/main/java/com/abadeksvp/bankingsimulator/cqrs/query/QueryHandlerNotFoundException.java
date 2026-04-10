package com.abadeksvp.bankingsimulator.cqrs.query;

public class QueryHandlerNotFoundException extends RuntimeException {

    public QueryHandlerNotFoundException(Class<?> queryType) {
        super("No handler registered for query: " + queryType.getName());
    }
}
