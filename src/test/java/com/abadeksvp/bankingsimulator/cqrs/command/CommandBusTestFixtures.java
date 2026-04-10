package com.abadeksvp.bankingsimulator.cqrs.command;

import com.abadeksvp.bankingsimulator.cqrs.core.AppError;
import com.abadeksvp.bankingsimulator.cqrs.core.ErrorCode;
import com.abadeksvp.bankingsimulator.cqrs.core.Result;

final class CommandBusTestFixtures {

    private CommandBusTestFixtures() {
    }

    record TestCommand(String input) implements Command<Void> {
    }

    record AnotherCommand(int value) implements Command<Void> {
    }

    record FailingCommand(String input) implements Command<Void> {
    }

    record UnregisteredCommand() implements Command<Void> {
    }

    enum TestErrorCode implements ErrorCode {
        TEST_ERROR;

        @Override
        public String code() {
            return name();
        }
    }

    static class TestCommandHandler extends AbstractCommandHandler<TestCommand, Void> {
        TestCommandHandler() {
            super(TestCommand.class);
        }

        @Override
        public Result<Void> handle(TestCommand command) {
            return Result.success(null);
        }
    }

    static class AnotherCommandHandler extends AbstractCommandHandler<AnotherCommand, Void> {
        AnotherCommandHandler() {
            super(AnotherCommand.class);
        }

        @Override
        public Result<Void> handle(AnotherCommand command) {
            return Result.success(null);
        }
    }

    static class FailingCommandHandler extends AbstractCommandHandler<FailingCommand, Void> {
        FailingCommandHandler() {
            super(FailingCommand.class);
        }

        @Override
        public Result<Void> handle(FailingCommand command) {
            return Result.failure(new AppError(TestErrorCode.TEST_ERROR, "something went wrong"));
        }
    }
}
