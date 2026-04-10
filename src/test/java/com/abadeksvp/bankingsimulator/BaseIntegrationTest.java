package com.abadeksvp.bankingsimulator;

import com.abadeksvp.bankingsimulator.common.Clock;
import com.abadeksvp.bankingsimulator.common.FixedClock;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.jdbc.Sql;

@Import({TestcontainersConfiguration.class, BaseIntegrationTest.TestClockConfiguration.class})
@SpringBootTest
@Sql(statements = "TRUNCATE accounts, transactions CASCADE", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public abstract class BaseIntegrationTest {

    protected static final Instant FIXED_INSTANT = Instant.parse("2025-01-15T10:00:00Z");

    @Autowired
    protected Clock clock;

    @TestConfiguration
    static class TestClockConfiguration {

        @Bean
        @Primary
        public Clock fixedClock() {
            return new FixedClock(FIXED_INSTANT);
        }
    }
}
