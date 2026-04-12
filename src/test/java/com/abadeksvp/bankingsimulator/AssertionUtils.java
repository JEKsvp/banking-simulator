package com.abadeksvp.bankingsimulator;

import static org.assertj.core.api.Assertions.assertThat;

public final class AssertionUtils {

    private AssertionUtils() {
    }

    public static void assertEqualsIgnoringFields(Object actual, Object expected, String... ignoredFields) {
        assertThat(actual)
                .usingRecursiveComparison()
                .ignoringFields(ignoredFields)
                .isEqualTo(expected);
    }
}
