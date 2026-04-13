package com.abadeksvp.bankingsimulator.domain.repository;

import com.abadeksvp.bankingsimulator.domain.model.Transaction;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends CrudRepository<Transaction, UUID> {

    List<Transaction> findBySourceAccountIdOrDestinationAccountId(UUID sourceAccountId, UUID destinationAccountId);

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    @Query("SELECT * FROM transactions WHERE id = :id FOR UPDATE")
    Optional<Transaction> findByIdForUpdate(UUID id);
}
