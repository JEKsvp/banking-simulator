package com.abadeksvp.bankingsimulator.cqrs.command;

import com.abadeksvp.bankingsimulator.cqrs.core.CqrsConfigurationException;
import com.abadeksvp.bankingsimulator.cqrs.core.Result;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class CommandBus {

    private final Map<Class<?>, CommandHandler<?>> handlers;
    private final CommandBusConfig config;
    private volatile ExecutorService executor;

    public CommandBus(List<CommandHandler<?>> handlerList, CommandBusConfig config) {
        this.config = config;
        this.handlers = new HashMap<>();
        for (CommandHandler<?> handler : handlerList) {
            Class<?> commandType = handler.getCommandType();
            if (handlers.containsKey(commandType)) {
                throw new CqrsConfigurationException(
                        "Duplicate handler for command: " + commandType.getName()
                );
            }
            handlers.put(commandType, handler);
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
    public <C extends Command> CompletableFuture<Result<Void>> dispatch(C command) {
        if (executor == null || executor.isShutdown()) {
            throw new IllegalStateException("CommandBus is not running. Call start() first.");
        }
        CommandHandler<C> handler = (CommandHandler<C>) handlers.get(command.getClass());
        if (handler == null) {
            throw new CommandHandlerNotFoundException(command.getClass());
        }
        return CompletableFuture.supplyAsync(() -> handler.handle(command), executor);
    }
}
