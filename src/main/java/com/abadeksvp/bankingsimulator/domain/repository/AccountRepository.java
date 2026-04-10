package com.abadeksvp.bankingsimulator.domain.repository;

import com.abadeksvp.bankingsimulator.domain.model.Account;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountRepository extends CrudRepository<Account, UUID> {

    Optional<Account> findByAccountNumber(String accountNumber);
}
