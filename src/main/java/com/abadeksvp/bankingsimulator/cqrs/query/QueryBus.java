package com.abadeksvp.bankingsimulator.cqrs.query;

import com.abadeksvp.bankingsimulator.cqrs.core.CqrsConfigurationException;
import com.abadeksvp.bankingsimulator.cqrs.core.Result;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class QueryBus {

    private final Map<Class<?>, QueryHandler<?, ?>> handlers;
    private final QueryBusConfig config;
    private volatile ExecutorService executor;

    public QueryBus(List<QueryHandler<?, ?>> handlerList, QueryBusConfig config) {
        this.config = config;
        this.handlers = new HashMap<>();
        for (QueryHandler<?, ?> handler : handlerList) {
            Class<?> queryType = handler.getQueryType();
            if (handlers.containsKey(queryType)) {
                throw new CqrsConfigurationException(
                        "Duplicate handler for query: " + queryType.getName()
                );
            }
            handlers.put(queryType, handler);
        }
    }

    public void start() {
        executor = Executors.newFixedThreadPool(config.threadPoolSize());
    }

    public void stop() {
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(config.shutdownTimeoutSeconds(), TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    public boolean isRunning() {
        return executor != null && !executor.isShutdown();
    }

    @SuppressWarnings("unchecked")
    public <R, Q extends Query<R>> CompletableFuture<Result<R>> dispatch(Q query) {
        if (executor == null || executor.isShutdown()) {
            throw new IllegalStateException("QueryBus is not running. Call start() first.");
        }
        QueryHandler<Q, R> handler = (QueryHandler<Q, R>) handlers.get(query.getClass());
        if (handler == null) {
            throw new QueryHandlerNotFoundException(query.getClass());
        }
        return CompletableFuture.supplyAsync(() -> handler.handle(query), executor);
    }
}
