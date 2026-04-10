package com.abadeksvp.bankingsimulator.cqrs.command;

import com.abadeksvp.bankingsimulator.cqrs.core.Result;

public interface CommandHandler<C extends Command<R>, R> {

    Class<C> getCommandType();

    Result<R> handle(C command);
}
