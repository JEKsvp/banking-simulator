package com.abadeksvp.bankingsimulator;

import org.springframework.boot.SpringApplication;

public class TestBankingSimulatorApplication {

    public static void main(String[] args) {
        SpringApplication.from(BankingSimulatorApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
