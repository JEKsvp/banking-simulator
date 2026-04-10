package com.abadeksvp.bankingsimulator.cqrs.core;

public sealed interface Result<T> {

    record Success<T>(T data) implements Result<T> {
    }

    record Failure<T>(AppError error) implements Result<T> {
    }

    static <T> Result<T> success(T data) {
        return new Success<>(data);
    }

    static <T> Result<T> failure(AppError error) {
        return new Failure<>(error);
    }

    default boolean isSuccess() {
        return this instanceof Success;
    }
}
