package com.abadeksvp.bankingsimulator.cqrs.command;

import com.abadeksvp.bankingsimulator.cqrs.command.CommandBusTestFixtures.AnotherCommandHandler;
import com.abadeksvp.bankingsimulator.cqrs.command.CommandBusTestFixtures.FailingCommandHandler;
import com.abadeksvp.bankingsimulator.cqrs.command.CommandBusTestFixtures.TestCommandHandler;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
class CommandHandlerTestConfiguration {

    @Bean
    TestCommandHandler testCommandHandler() {
        return new TestCommandHandler();
    }

    @Bean
    AnotherCommandHandler anotherCommandHandler() {
        return new AnotherCommandHandler();
    }

    @Bean
    FailingCommandHandler failingCommandHandler() {
        return new FailingCommandHandler();
    }
}
