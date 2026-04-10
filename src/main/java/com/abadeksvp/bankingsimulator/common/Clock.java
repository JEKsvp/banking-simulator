package com.abadeksvp.bankingsimulator.common;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class Clock {

    public Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MICROS);
    }
}
