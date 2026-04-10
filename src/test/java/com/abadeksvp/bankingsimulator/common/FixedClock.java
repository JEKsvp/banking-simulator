package com.abadeksvp.bankingsimulator.common;

import java.time.Instant;

public class FixedClock extends Clock {

    private final Instant fixedInstant;

    public FixedClock(Instant fixedInstant) {
        this.fixedInstant = fixedInstant;
    }

    @Override
    public Instant now() {
        return fixedInstant;
    }
}
