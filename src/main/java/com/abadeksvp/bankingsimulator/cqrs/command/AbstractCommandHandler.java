package com.abadeksvp.bankingsimulator.cqrs.command;

public abstract class AbstractCommandHandler<C extends Command<R>, R> implements CommandHandler<C, R> {

    private final Class<C> commandType;

    protected AbstractCommandHandler(Class<C> commandType) {
        this.commandType = commandType;
    }

    @Override
    public Class<C> getCommandType() {
        return commandType;
    }
}
