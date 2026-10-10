# 0011 — Kafka excluded from the AWS deployment

**Status:** Accepted

## Context

Phase 8 added Kafka, a transactional outbox, and two consumers, all running locally via
docker-compose. A managed equivalent on AWS (MSK) exists, but bills continuously for the
broker's existence, not for what passes through it — real money accruing every hour regardless
of whether anyone is looking at the demo.

## Decision

The AWS deployment runs the application with `SPRING_PROFILES_ACTIVE=cloud`, which activates
`application-cloud.yaml` and, via `@Profile("!cloud")` on their own class declarations, excludes
`OutboxRelay`, `AuditLogConsumer`, and `NotificationConsumer` entirely. `TransferService` still
writes `outbox_events` rows in every environment, unconditionally — those rows simply accumulate
unpublished in the cloud deployment, since nothing is polling for them there.

## Why not just leave Kafka running and eat the cost

A managed Kafka cluster costs real money per hour of existence, independent of message volume —
for a system meant to receive occasional interview-reviewer traffic rather than continuous
production load, that's a cost disproportionate to the value it demonstrates in this specific
deployment. The event-driven architecture itself is already fully proven out locally (Phase 8's
tests, including a real observed duplicate-delivery-handled-correctly run); re-proving it again
against a second, paid Kafka cluster doesn't add new evidence of correctness.

## Why `TransferService` doesn't get an `if (!cloudProfile)` branch of its own

Keeping the outbox write unconditional means there is exactly one code path for the thing that
actually matters — money moving — in every environment. Branching the core transfer logic based
on which environment it's running in would mean the cloud deployment and the local/test
environment are no longer running the same code, which is exactly the kind of divergence that
hides environment-specific bugs. Disabling only the *consumers* of the outbox (which have no
bearing on whether a transfer itself is correct) keeps that one path intact everywhere.

## Consequence

The AWS deployment has no audit trail and no notification feature — a real, documented scope
boundary, not a silent gap. A reviewer poking at the deployed demo won't see those features;
they're fully demonstrated locally instead, which is the more honest and more easily verified
place for them to live anyway (see Phase 8's test suite, which runs against a real Kafka broker
via Testcontainers on every CI run).
