package com.abadeksvp.bankingsimulator.cqrs.query;

import com.abadeksvp.bankingsimulator.cqrs.core.AppError;
import com.abadeksvp.bankingsimulator.cqrs.core.ErrorCode;
import com.abadeksvp.bankingsimulator.cqrs.core.Result;

final class QueryBusTestFixtures {

    private QueryBusTestFixtures() {
    }

    record TestQuery(String input) implements Query<String> {
    }

    record AnotherQuery(int value) implements Query<Integer> {
    }

    record FailingQuery(String input) implements Query<String> {
    }

    record UnregisteredQuery() implements Query<String> {
    }

    enum TestErrorCode implements ErrorCode {
        TEST_ERROR;

        @Override
        public String code() {
            return name();
        }
    }

    static class TestQueryHandler extends AbstractQueryHandler<TestQuery, String> {
        TestQueryHandler() {
            super(TestQuery.class);
        }

        @Override
        public Result<String> handle(TestQuery query) {
            return Result.success("result: " + query.input());
        }
    }

    static class AnotherQueryHandler extends AbstractQueryHandler<AnotherQuery, Integer> {
        AnotherQueryHandler() {
            super(AnotherQuery.class);
        }

        @Override
        public Result<Integer> handle(AnotherQuery query) {
            return Result.success(query.value() * 2);
        }
    }

    static class FailingQueryHandler extends AbstractQueryHandler<FailingQuery, String> {
        FailingQueryHandler() {
            super(FailingQuery.class);
        }

        @Override
        public Result<String> handle(FailingQuery query) {
            return Result.failure(new AppError(TestErrorCode.TEST_ERROR, "query failed"));
        }
    }
}
