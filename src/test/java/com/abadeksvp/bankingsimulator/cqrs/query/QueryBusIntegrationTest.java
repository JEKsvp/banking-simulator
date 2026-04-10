package com.abadeksvp.bankingsimulator.cqrs.query;

import com.abadeksvp.bankingsimulator.BaseIntegrationTest;
import com.abadeksvp.bankingsimulator.cqrs.core.AppError;
import com.abadeksvp.bankingsimulator.cqrs.core.Result;
import com.abadeksvp.bankingsimulator.cqrs.query.QueryBusTestFixtures.AnotherQuery;
import com.abadeksvp.bankingsimulator.cqrs.query.QueryBusTestFixtures.FailingQuery;
import com.abadeksvp.bankingsimulator.cqrs.query.QueryBusTestFixtures.TestErrorCode;
import com.abadeksvp.bankingsimulator.cqrs.query.QueryBusTestFixtures.TestQuery;
import com.abadeksvp.bankingsimulator.cqrs.query.QueryBusTestFixtures.UnregisteredQuery;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(QueryHandlerTestConfiguration.class)
class QueryBusIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private QueryBus queryBus;

    @Test
    void shouldDispatchQuery() throws Exception {
        Result<String> result = queryBus.dispatch(new TestQuery("hello")).get();

        assertThat(result).isEqualTo(Result.success("result: hello"));
    }

    @Test
    void shouldDispatchMultipleQueryTypes() throws Exception {
        Result<Integer> result = queryBus.dispatch(new AnotherQuery(5)).get();

        assertThat(result).isEqualTo(Result.success(10));
    }

    @Test
    void shouldThrowForUnregisteredQuery() {
        assertThatThrownBy(() -> queryBus.dispatch(new UnregisteredQuery()))
                .isInstanceOf(QueryHandlerNotFoundException.class)
                .hasMessageContaining("No handler registered for query");
    }

    @Test
    void shouldReturnFailureResult() throws Exception {
        Result<String> result = queryBus.dispatch(new FailingQuery("hello")).get();

        assertThat(result).isEqualTo(Result.failure(new AppError(TestErrorCode.TEST_ERROR, "query failed")));
    }

    @Test
    void shouldBeStartedBySpringLifecycle() {
        assertThat(queryBus.isRunning()).isTrue();
    }
}
