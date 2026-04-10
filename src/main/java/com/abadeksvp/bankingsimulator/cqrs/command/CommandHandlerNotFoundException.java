package com.abadeksvp.bankingsimulator.cqrs.command;

public class CommandHandlerNotFoundException extends RuntimeException {

    public CommandHandlerNotFoundException(Class<?> commandType) {
        super("No handler registered for command: " + commandType.getName());
    }
}
