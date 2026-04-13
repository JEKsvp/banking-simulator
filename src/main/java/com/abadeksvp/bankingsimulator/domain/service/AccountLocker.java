package com.abadeksvp.bankingsimulator.domain.service;

import com.abadeksvp.bankingsimulator.domain.error.AccountNotFoundException;
import com.abadeksvp.bankingsimulator.domain.model.Account;
import com.abadeksvp.bankingsimulator.domain.repository.AccountRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AccountLocker {

    private final AccountRepository accountRepository;

    public AccountLocker(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public Account lockAccount(UUID accountId) {
        return accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
    }

    public AccountPair lockForTransaction(UUID sourceId, UUID destinationId) {
        UUID firstId;
        UUID secondId;
        if (sourceId.compareTo(destinationId) <= 0) {
            firstId = sourceId;
            secondId = destinationId;
        } else {
            firstId = destinationId;
            secondId = sourceId;
        }

        Account first = accountRepository.findByIdForUpdate(firstId)
                .orElseThrow(() -> new AccountNotFoundException(firstId));

        Account second = accountRepository.findByIdForUpdate(secondId)
                .orElseThrow(() -> new AccountNotFoundException(secondId));

        Account source = first.getId().equals(sourceId) ? first : second;
        Account destination = first.getId().equals(destinationId) ? first : second;

        return new AccountPair(source, destination);
    }

    public record AccountPair(Account source, Account destination) {
    }
}
