package com.abadeksvp.bankingsimulator.cqrs.command;

public abstract class AbstractCommandHandler<C extends Command> implements CommandHandler<C> {

    private final Class<C> commandType;

    protected AbstractCommandHandler(Class<C> commandType) {
        this.commandType = commandType;
    }

    @Override
    public Class<C> getCommandType() {
        return commandType;
    }
}
