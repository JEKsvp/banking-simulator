package com.abadeksvp.bankingsimulator.domain.repository;

import com.abadeksvp.bankingsimulator.domain.model.Account;
import com.abadeksvp.bankingsimulator.domain.model.AccountType;
import com.abadeksvp.bankingsimulator.domain.model.Currency;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountRepository extends CrudRepository<Account, UUID> {

    Optional<Account> findByAccountNumber(String accountNumber);

    Optional<Account> findByTypeAndCurrency(AccountType type, Currency currency);

    @Query("SELECT * FROM accounts WHERE id = :id FOR UPDATE")
    Optional<Account> findByIdForUpdate(UUID id);
}
