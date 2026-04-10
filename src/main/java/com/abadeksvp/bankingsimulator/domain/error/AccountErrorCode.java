package com.abadeksvp.bankingsimulator.domain.error;

import com.abadeksvp.bankingsimulator.cqrs.core.ErrorCode;

public enum AccountErrorCode implements ErrorCode {

    ACCOUNT_ALREADY_EXISTS,
    ACCOUNT_NOT_FOUND;

    @Override
    public String code() {
        return name();
    }
}
