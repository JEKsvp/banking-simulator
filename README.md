# Banking Simulator

Banking operations service: account creation, deposits, withdrawals, transfers, and balance queries. Spring Boot 4.0.5 / Java 25 / Spring Data JDBC / PostgreSQL / Flyway.

## Prerequisites

- **Docker** (tests use Testcontainers to spin up PostgreSQL automatically)
- **Java 25** (Gradle toolchain downloads it if missing)

```bash
./gradlew build   # build + run all tests
./gradlew test    # tests only
./gradlew bootRun # start the app
```

## Feature Map

All operations are CQRS commands/queries in `domain/command/` and `domain/query/`.

Deposits and withdrawals use **double-entry bookkeeping**: every money movement is a transfer between two accounts. A per-currency **system account** (`CreateSystemAccountCommand`) acts as the external counterparty -- deposits transfer from the system account to the user, withdrawals transfer from the user back to the system account. This keeps the ledger zero-sum at all times.

| Requirement | Command / Query | Handler |
|---|---|---|
| Account creation (with optional initial deposit) | `CreateUserAccountCommand` | `CreateUserAccountCommandHandler` |
| Deposit | `DepositCommand` | `DepositCommandHandler` |
| Withdrawal | `WithdrawCommand` | `WithdrawCommandHandler` |
| Transfer | `TransferCommand` | `TransferCommandHandler` |
| Balance check | `GetAccountQuery` | `GetAccountQueryHandler` |

## Project Structure

```
domain/
  model/        Account, Transaction, Money, Currency, TransactionStatus
  command/      Commands + handlers (create account, deposit, withdraw, transfer)
  query/        GetAccountQuery + handler, AccountView projection
  service/      TransactionProcessor (orchestration), AccountLocker (pessimistic locking)
  repository/   AccountRepository, TransactionRepository (Spring Data JDBC)
  error/        Error codes and domain exceptions

cqrs/           CommandBus, QueryBus (async dispatch), Result<T> sealed type, AppError

resources/
  db/migration/ Flyway migrations (V1-V5)
```

## Tech Stack

Spring Boot 4.0.5, Java 25, Spring Data JDBC (no JPA), PostgreSQL, Flyway, Testcontainers, JUnit 5, AssertJ, Lombok, Gradle 9.4
