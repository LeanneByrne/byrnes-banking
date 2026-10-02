# ByrnesBanking — Engineering Rules

Mock core banking API: Java 21, Spring Boot, Maven, PostgreSQL, Flyway,
Spring Security + JWT, JUnit 5, Testcontainers. No Lombok.

## Rules

- Package by feature: `account`, `customer`, `transfer`, `ledger`, `security`, `common`.
- Money is stored as `long` cents, never `double` or `float`.
- Ledger entries are immutable; corrections use reversing entries.
- Balances are derived from the ledger, never stored.
- Every transfer writes one debit and one equal credit in one transaction.
- Transfers require an `Idempotency-Key` header.
- Lock accounts in a consistent order to prevent deadlocks.
- Flyway owns the schema; Hibernate `ddl-auto` is `validate`.
- DTOs (records) at the API boundary; never return entities.
- Errors return `ProblemDetail` via a global exception handler.
- No secrets in code; use environment variables.
- Every feature needs tests; run `mvn test` before calling anything done.

## Git workflow

- Feature branches off `main`, named for the work (e.g. `feat/accounts-ledger`).
- Commits follow Conventional Commits (`feat:`, `fix:`, `test:`, `docs:`, `refactor:`, `chore:`) and stay small and focused.
- Every change merges through a Pull Request with a description covering what changed, why, and how it was tested.
- CI (`./mvnw verify`) must pass on the PR before merging.
