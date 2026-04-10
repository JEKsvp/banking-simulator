package com.abadeksvp.bankingsimulator.config;

import com.abadeksvp.bankingsimulator.common.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfiguration {

    @Bean
    public Clock clock() {
        return new Clock();
    }
}
