package com.abadeksvp.bankingsimulator.cqrs.command;

import com.abadeksvp.bankingsimulator.cqrs.core.Result;

public interface CommandHandler<C extends Command> {

    Class<C> getCommandType();

    Result<Void> handle(C command);
}
