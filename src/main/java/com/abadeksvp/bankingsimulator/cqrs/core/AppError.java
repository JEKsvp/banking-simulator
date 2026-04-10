package com.abadeksvp.bankingsimulator.cqrs.core;

import lombok.Getter;

import java.util.Objects;

@Getter
public class AppError extends RuntimeException {

    private final ErrorCode errorCode;

    public AppError(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AppError other)) return false;
        return Objects.equals(errorCode, other.errorCode)
                && Objects.equals(getMessage(), other.getMessage());
    }

    @Override
    public int hashCode() {
        return Objects.hash(errorCode, getMessage());
    }
}
