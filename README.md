# ByrnesBanking

[![CI](https://github.com/LeanneByrne/byrnes-banking/actions/workflows/ci.yml/badge.svg)](https://github.com/LeanneByrne/byrnes-banking/actions/workflows/ci.yml)
[![CodeQL](https://github.com/LeanneByrne/byrnes-banking/actions/workflows/codeql.yml/badge.svg)](https://github.com/LeanneByrne/byrnes-banking/actions/workflows/codeql.yml)
![coverage](https://img.shields.io/badge/coverage-91%25%20lines%20%2F%2084%25%20branches-brightgreen)
![license](https://img.shields.io/badge/license-MIT-blue)

A mock core banking API: customers, accounts, and idempotent money transfers backed by an
immutable double-entry ledger. Built to demonstrate the engineering hygiene a real banking
backend needs — correct money handling, concurrency-safe transfers, auditability, and tests
that actually prove the hard invariants hold under contention.

> **This is a demo, not a real bank.** No real money, no real customers, no connection to any
> financial institution. Seed data uses fake people and fake balances purely so the API has
> something realistic to operate on.

## Stack

Java 21 · Spring Boot 4 · Maven · PostgreSQL · Flyway · Spring Security (JWT) · Apache Kafka ·
Micrometer · Prometheus · Grafana · JUnit 5 · Testcontainers · JaCoCo. No Lombok — DTOs are plain
records, entities are explicit.

## Why this exists

Most portfolio CRUD apps don't have to get money math right. This one does, on purpose:

- **Money is `long` cents, never `double`/`float`/`BigDecimal`** — floating point can't
  represent currency exactly, and that's not a theoretical problem, it's a guaranteed one.
- **Balances are derived from the ledger, not stored.** A balance column can drift from the
  transaction history that's supposed to justify it. Summing the ledger can't drift from itself.
- **Every transfer writes one debit and one equal credit in a single transaction.** Classic
  double-entry bookkeeping — money never appears or disappears, it only moves.
- **Ledger entries are immutable forever.** No update or delete path exists for them, anywhere
  in the code. Corrections are new reversing entries, never edits — so the ledger is always a
  truthful history of what actually happened, including mistakes.
- **Transfers require an idempotency key and are concurrency-safe under real contention** —
  proven with a test that fires 20 simultaneous transfer attempts against a fixed balance and
  asserts the ledger still nets to zero afterward, not just "it looked fine."

The reasoning behind each of these is written up as a short ADR in
[`docs/decisions/`](docs/decisions/) — see [Design decisions](#design-decisions) below.

## Running it locally

```bash
cp .env.example .env          # demo defaults work out of the box, no edits required
docker compose up -d          # Postgres, Kafka, Prometheus, and Grafana
./mvnw spring-boot:run         # Flyway migrates + seeds on startup, app listens on :8080
```

Run the test suite (unit + Testcontainers integration tests, including the concurrency proof):

```bash
./mvnw verify
```

## Demo logins

Seed data ([`V2__seed_demo_data.sql`](src/main/resources/db/migration/V2__seed_demo_data.sql))
creates three customers, each with two accounts and realistic transaction history, so you can
log in immediately without registering:

| Email | Password | Role |
|---|---|---|
| `alice@byrnesbanking.dev` | `Demo1234!` | CUSTOMER |
| `ben@byrnesbanking.dev` | `Demo1234!` | CUSTOMER |
| `priya@byrnesbanking.dev` | `Demo1234!` | CUSTOMER |

Setting `ADMIN_EMAIL`/`ADMIN_PASSWORD` in `.env` (defaults already set in `.env.example`) seeds
an ADMIN account on startup, which can list all customers via `GET /api/customers`.

## API

Full interactive API docs (OpenAPI/Swagger) are served by the running app at
**http://localhost:8080/swagger-ui/index.html** — no auth required to browse the docs
themselves, though every endpoint except `/api/auth/**` needs a bearer token to call.

### Log in and call an authenticated endpoint

```bash
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"alice@byrnesbanking.dev","password":"Demo1234!"}' | jq -r .token)

curl -s localhost:8080/api/accounts/00000000-0000-0000-0000-000000000111 \
  -H "Authorization: Bearer $TOKEN"
```

### Make a transfer (idempotent — safe to retry with the same key)

```bash
curl -s -X POST localhost:8080/api/transfers \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: $(uuidgen)" \
  -d '{
        "sourceAccountId": "00000000-0000-0000-0000-000000000111",
        "destinationAccountId": "00000000-0000-0000-0000-000000000211",
        "amountCents": 2500
      }'
```

### Page through an account's transaction history

```bash
curl -s "localhost:8080/api/accounts/00000000-0000-0000-0000-000000000111/transactions?page=0&size=10" \
  -H "Authorization: Bearer $TOKEN"
```

Trying any of these against another customer's account returns `403 Forbidden` — ownership is
enforced in the service layer, not just by hiding the button in a UI that doesn't exist here.

## Architecture

See [`docs/architecture.md`](docs/architecture.md) for a diagram and walkthrough of the request
flow: JWT filter → controller → service → ordered account locking → ledger write → Postgres.

## Observability

- **Health:** `GET /actuator/health` (public, for load-balancer-style probes)
- **Metrics:** `GET /actuator/prometheus` (public, scraped by Prometheus) — custom metrics include
  `transfers_completed_total`, `transfers_failed_total{reason=...}`, and a `transfers_duration_seconds`
  histogram. Everything else under `/actuator/**` requires an ADMIN token — see
  [ADR 0009](docs/decisions/0009-actuator-exposure.md) for why the boundary sits there.
- **Dashboard:** once `docker compose up -d` is running, open **http://localhost:3000**
  (anonymous viewer access enabled) — Grafana auto-provisions a Prometheus datasource and a
  "ByrnesBanking" dashboard (transfer rate, failure rate by reason, latency percentiles, JVM
  heap) with zero manual setup.
- **Logs** are structured JSON (Elastic Common Schema) on the console, and every response
  carries an `X-Trace-Id` header matching Micrometer Tracing's trace id for that request — grep
  the logs for that id to pull the complete story of one specific request out of everything else
  running concurrently.

## Design decisions

Short ADRs for the calls that would otherwise get silently re-litigated or reversed by someone
(including future me) who didn't see the original reasoning:

- [0001 — Money as integer cents](docs/decisions/0001-money-as-cents.md)
- [0002 — Ledger-derived balances](docs/decisions/0002-ledger-derived-balances.md)
- [0003 — Double-entry invariant](docs/decisions/0003-double-entry-invariant.md)
- [0004 — Idempotency key strategy](docs/decisions/0004-idempotency-key-strategy.md)
- [0005 — Consistent-order account locking](docs/decisions/0005-consistent-order-account-locking.md)
- [0006 — Flyway owns the schema](docs/decisions/0006-flyway-owns-schema.md)
- [0007 — Stateless JWT auth design](docs/decisions/0007-jwt-auth-design.md)
- [0008 — Transactional outbox with idempotent, at-least-once consumers](docs/decisions/0008-transactional-outbox.md)
- [0009 — Actuator exposure boundary](docs/decisions/0009-actuator-exposure.md)

## Known, deliberate limitations

- **The coverage badge above is a static snapshot**, not a live-updating one — there's no
  Codecov/Coveralls integration wired up. The numbers are from the last `./mvnw verify` run's
  JaCoCo report (`target/site/jacoco/index.html` after running it yourself).
- **A revoked/disabled customer's existing JWT stays valid until it expires** — rechecking
  `enabled` on every request would mean a DB lookup per request, defeating the point of
  stateless auth. Documented as an accepted tradeoff, not an oversight.
- **Prometheus scrapes the app unauthenticated** — acceptable because it's only reachable over
  the docker-compose-internal network here, not a real production network boundary. See
  [ADR 0009](docs/decisions/0009-actuator-exposure.md).

## License

MIT — see [LICENSE](LICENSE).
