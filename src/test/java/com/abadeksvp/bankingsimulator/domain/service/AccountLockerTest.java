package com.abadeksvp.bankingsimulator.domain.service;

import com.abadeksvp.bankingsimulator.domain.error.AccountNotFoundException;
import com.abadeksvp.bankingsimulator.domain.model.Account;
import com.abadeksvp.bankingsimulator.domain.model.AccountType;
import com.abadeksvp.bankingsimulator.domain.model.Currency;
import com.abadeksvp.bankingsimulator.domain.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountLockerTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountLocker accountLocker;

    private static final Instant NOW = Instant.parse("2025-01-15T10:00:00Z");

    private static final UUID SMALLER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID LARGER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test
    void shouldLockSingleAccount() {
        Account account = buildAccount(SMALLER_ID);

        when(accountRepository.findByIdForUpdate(SMALLER_ID)).thenReturn(Optional.of(account));

        Account result = accountLocker.lockAccount(SMALLER_ID);

        assertThat(result).isSameAs(account);
    }

    @Test
    void shouldThrowWhenSingleAccountNotFound() {
        when(accountRepository.findByIdForUpdate(SMALLER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountLocker.lockAccount(SMALLER_ID))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessage("Account with id %s not found".formatted(SMALLER_ID));
    }

    @Test
    void shouldLockSmallerUuidFirst() {
        Account smallerAccount = buildAccount(SMALLER_ID);
        Account largerAccount = buildAccount(LARGER_ID);

        when(accountRepository.findByIdForUpdate(SMALLER_ID)).thenReturn(Optional.of(smallerAccount));
        when(accountRepository.findByIdForUpdate(LARGER_ID)).thenReturn(Optional.of(largerAccount));

        accountLocker.lockForTransaction(LARGER_ID, SMALLER_ID);

        InOrder order = inOrder(accountRepository);
        order.verify(accountRepository).findByIdForUpdate(SMALLER_ID);
        order.verify(accountRepository).findByIdForUpdate(LARGER_ID);
    }

    @Test
    void shouldReturnCorrectSourceAndDestinationWhenSourceIsSmaller() {
        Account sourceAccount = buildAccount(SMALLER_ID);
        Account destinationAccount = buildAccount(LARGER_ID);

        when(accountRepository.findByIdForUpdate(SMALLER_ID)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByIdForUpdate(LARGER_ID)).thenReturn(Optional.of(destinationAccount));

        AccountLocker.AccountPair pair = accountLocker.lockForTransaction(SMALLER_ID, LARGER_ID);

        assertThat(pair.source()).isSameAs(sourceAccount);
        assertThat(pair.destination()).isSameAs(destinationAccount);
    }

    @Test
    void shouldReturnCorrectSourceAndDestinationWhenDestinationIsSmaller() {
        Account sourceAccount = buildAccount(LARGER_ID);
        Account destinationAccount = buildAccount(SMALLER_ID);

        when(accountRepository.findByIdForUpdate(SMALLER_ID)).thenReturn(Optional.of(destinationAccount));
        when(accountRepository.findByIdForUpdate(LARGER_ID)).thenReturn(Optional.of(sourceAccount));

        AccountLocker.AccountPair pair = accountLocker.lockForTransaction(LARGER_ID, SMALLER_ID);

        assertThat(pair.source()).isSameAs(sourceAccount);
        assertThat(pair.destination()).isSameAs(destinationAccount);
    }

    @Test
    void shouldThrowWhenFirstLockedAccountNotFound() {
        when(accountRepository.findByIdForUpdate(SMALLER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountLocker.lockForTransaction(SMALLER_ID, LARGER_ID))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessage("Account with id %s not found".formatted(SMALLER_ID));
    }

    @Test
    void shouldThrowWhenSecondLockedAccountNotFound() {
        Account smallerAccount = buildAccount(SMALLER_ID);

        when(accountRepository.findByIdForUpdate(SMALLER_ID)).thenReturn(Optional.of(smallerAccount));
        when(accountRepository.findByIdForUpdate(LARGER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountLocker.lockForTransaction(SMALLER_ID, LARGER_ID))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessage("Account with id %s not found".formatted(LARGER_ID));
    }

    private Account buildAccount(UUID id) {
        return Account.builder()
                .id(id)
                .accountNumber("ACC-" + id.toString().substring(0, 8))
                .userId(UUID.randomUUID())
                .type(AccountType.USER)
                .currency(Currency.USD)
                .createdAt(NOW)
                .updatedAt(NOW)
                .build();
    }
}
