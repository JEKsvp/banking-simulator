package com.abadeksvp.bankingsimulator.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Currency {

    USD("US Dollar"),
    EUR("Euro"),
    GBP("British Pound");

    private final String displayName;
}
