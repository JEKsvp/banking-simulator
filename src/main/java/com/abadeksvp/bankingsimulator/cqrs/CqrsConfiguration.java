package com.abadeksvp.bankingsimulator.cqrs;

import com.abadeksvp.bankingsimulator.cqrs.command.CommandBus;
import com.abadeksvp.bankingsimulator.cqrs.command.CommandBusConfig;
import com.abadeksvp.bankingsimulator.cqrs.command.CommandHandler;
import com.abadeksvp.bankingsimulator.cqrs.query.QueryBus;
import com.abadeksvp.bankingsimulator.cqrs.query.QueryBusConfig;
import com.abadeksvp.bankingsimulator.cqrs.query.QueryHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class CqrsConfiguration {

    @Bean
    public CommandBusConfig commandBusConfig() {
        return CommandBusConfig.builder().build();
    }

    @Bean
    public QueryBusConfig queryBusConfig() {
        return QueryBusConfig.builder().build();
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public CommandBus commandBus(List<CommandHandler<?, ?>> handlers, CommandBusConfig commandBusConfig) {
        return new CommandBus(handlers, commandBusConfig);
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public QueryBus queryBus(List<QueryHandler<?, ?>> handlers, QueryBusConfig queryBusConfig) {
        return new QueryBus(handlers, queryBusConfig);
    }
}
