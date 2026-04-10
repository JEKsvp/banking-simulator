package com.abadeksvp.bankingsimulator.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionStatusTest {

    @ParameterizedTest
    @MethodSource("allowedTransitions")
    void shouldAllowValidTransitions(TransactionStatus from, TransactionStatus to) {
        assertThat(from.canTransitionTo(to)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("disallowedTransitions")
    void shouldDisallowInvalidTransitions(TransactionStatus from, TransactionStatus to) {
        assertThat(from.canTransitionTo(to)).isFalse();
    }

    @Test
    void shouldNotAllowSelfTransitions() {
        for (TransactionStatus status : TransactionStatus.values()) {
            assertThat(status.canTransitionTo(status)).isFalse();
        }
    }

    static Stream<Arguments> allowedTransitions() {
        return Stream.of(
                Arguments.of(TransactionStatus.CREATED, TransactionStatus.PENDING),
                Arguments.of(TransactionStatus.CREATED, TransactionStatus.DECLINED),
                Arguments.of(TransactionStatus.PENDING, TransactionStatus.COMPLETED),
                Arguments.of(TransactionStatus.PENDING, TransactionStatus.DECLINED)
        );
    }

    static Stream<Arguments> disallowedTransitions() {
        return Stream.of(
                Arguments.of(TransactionStatus.CREATED, TransactionStatus.COMPLETED),
                Arguments.of(TransactionStatus.PENDING, TransactionStatus.CREATED),
                Arguments.of(TransactionStatus.COMPLETED, TransactionStatus.CREATED),
                Arguments.of(TransactionStatus.COMPLETED, TransactionStatus.PENDING),
                Arguments.of(TransactionStatus.COMPLETED, TransactionStatus.DECLINED),
                Arguments.of(TransactionStatus.DECLINED, TransactionStatus.CREATED),
                Arguments.of(TransactionStatus.DECLINED, TransactionStatus.PENDING),
                Arguments.of(TransactionStatus.DECLINED, TransactionStatus.COMPLETED)
        );
    }
}
