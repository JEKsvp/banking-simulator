package com.abadeksvp.bankingsimulator.domain.error;

import com.abadeksvp.bankingsimulator.cqrs.core.ErrorCode;

public enum TransactionErrorCode implements ErrorCode {

    INSUFFICIENT_FUNDS,
    CURRENCY_MISMATCH,
    SYSTEM_ACCOUNT_NOT_FOUND,
    TRANSFER_NOT_ALLOWED,
    DEPOSIT_NOT_ALLOWED,
    WITHDRAWAL_NOT_ALLOWED;

    @Override
    public String code() {
        return name();
    }
}
