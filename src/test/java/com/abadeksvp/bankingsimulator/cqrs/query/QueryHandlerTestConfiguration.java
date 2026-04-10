package com.abadeksvp.bankingsimulator.cqrs.query;

import com.abadeksvp.bankingsimulator.cqrs.query.QueryBusTestFixtures.AnotherQueryHandler;
import com.abadeksvp.bankingsimulator.cqrs.query.QueryBusTestFixtures.FailingQueryHandler;
import com.abadeksvp.bankingsimulator.cqrs.query.QueryBusTestFixtures.TestQueryHandler;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
class QueryHandlerTestConfiguration {

    @Bean
    TestQueryHandler testQueryHandler() {
        return new TestQueryHandler();
    }

    @Bean
    AnotherQueryHandler anotherQueryHandler() {
        return new AnotherQueryHandler();
    }

    @Bean
    FailingQueryHandler failingQueryHandler() {
        return new FailingQueryHandler();
    }
}
