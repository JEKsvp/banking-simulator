package com.abadeksvp.bankingsimulator.domain.model;

import java.util.Map;
import java.util.Set;

public enum TransactionStatus {

    PENDING,
    COMPLETED,
    DECLINED;

    private static final Map<TransactionStatus, Set<TransactionStatus>> ALLOWED_TRANSITIONS = Map.of(
            PENDING, Set.of(COMPLETED, DECLINED),
            COMPLETED, Set.of(),
            DECLINED, Set.of()
    );

    public boolean canTransitionTo(TransactionStatus target) {
        return ALLOWED_TRANSITIONS.get(this).contains(target);
    }
}
