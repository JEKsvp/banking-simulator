package com.abadeksvp.bankingsimulator.cqrs.command;

import com.abadeksvp.bankingsimulator.BaseIntegrationTest;
import com.abadeksvp.bankingsimulator.cqrs.command.CommandBusTestFixtures.AnotherCommand;
import com.abadeksvp.bankingsimulator.cqrs.command.CommandBusTestFixtures.FailingCommand;
import com.abadeksvp.bankingsimulator.cqrs.command.CommandBusTestFixtures.TestCommand;
import com.abadeksvp.bankingsimulator.cqrs.command.CommandBusTestFixtures.TestErrorCode;
import com.abadeksvp.bankingsimulator.cqrs.command.CommandBusTestFixtures.UnregisteredCommand;
import com.abadeksvp.bankingsimulator.cqrs.core.AppError;
import com.abadeksvp.bankingsimulator.cqrs.core.Result;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(CommandHandlerTestConfiguration.class)
class CommandBusIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private CommandBus commandBus;

    @Test
    void shouldDispatchCommand() throws Exception {
        Result<Void> result = commandBus.dispatch(new TestCommand("hello")).get();

        assertThat(result).isEqualTo(Result.success(null));
    }

    @Test
    void shouldDispatchMultipleCommandTypes() throws Exception {
        Result<Void> result = commandBus.dispatch(new AnotherCommand(5)).get();

        assertThat(result).isEqualTo(Result.success(null));
    }

    @Test
    void shouldThrowForUnregisteredCommand() {
        assertThatThrownBy(() -> commandBus.dispatch(new UnregisteredCommand()))
                .isInstanceOf(CommandHandlerNotFoundException.class)
                .hasMessageContaining("No handler registered for command");
    }

    @Test
    void shouldReturnFailureResult() throws Exception {
        Result<Void> result = commandBus.dispatch(new FailingCommand("hello")).get();

        assertThat(result).isEqualTo(Result.failure(new AppError(TestErrorCode.TEST_ERROR, "something went wrong")));
    }

    @Test
    void shouldReportSuccessCorrectly() {
        assertThat(Result.success(null).isSuccess()).isTrue();
        assertThat(Result.failure(new AppError(TestErrorCode.TEST_ERROR, "fail")).isSuccess()).isFalse();
    }

    @Test
    void shouldBeStartedBySpringLifecycle() {
        assertThat(commandBus.isRunning()).isTrue();
    }
}
