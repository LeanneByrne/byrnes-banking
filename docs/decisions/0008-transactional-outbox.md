# 0008 — Transactional outbox with idempotent, at-least-once consumers

**Status:** Accepted

## Context

Other parts of the system (an audit trail, customer notifications, and eventually things like
fraud detection or reporting) need to react to a completed transfer without being part of the
transfer's own request/response cycle. The natural-looking approach — after saving the transfer,
call Kafka directly to publish an event — has no atomicity between the two operations.

## Decision

`TransferService` writes an `outbox_events` row in the **same database transaction** as the
transfer and ledger rows. A separate `OutboxRelay`, running on a schedule, polls for unpublished
rows and publishes them to Kafka, marking each one published only after a successful send. Event
metadata (`eventId`, `eventType`) travels as Kafka record headers; the message value is exactly
the business payload. Consumers (`AuditLogConsumer`, `NotificationConsumer`) are required to be
idempotent — see below.

## Why not publish to Kafka directly from the transfer request

Two operations with no shared atomicity means two ways to be wrong: commit the transfer, then
crash or fail before the Kafka publish, and the event is lost forever with no record it should
have existed. Or publish first, then have the surrounding transaction roll back, and consumers
now react to a transfer that never actually happened. Making the outbox row part of the same
transaction as the transfer closes both gaps by construction: either everything in that
transaction commits together, or none of it does.

## Why this only gives *at-least-once* delivery, and why that's the right guarantee here

The relay can crash or time out after Kafka acknowledges a send but before the row is marked
published — the next poll would then publish the same row again. Rather than trying to eliminate
this window (which would require a distributed transaction across Postgres and Kafka, a much
heavier mechanism for a demo-scale system), the design accepts it and pushes the requirement onto
consumers instead: **every consumer of these events must be idempotent**, meaning processing the
same event twice produces the same result as processing it once. This was directly exercised, not
just assumed — `TransferEventPipelineIntegrationTest` has observed a real duplicate delivery
during its own run (a genuine `23505 duplicate key` conflict on `notification_log`'s unique
constraint, caught and logged, not thrown) before the suite went green, which means the whole
point of this design actually happened on real infrastructure, not only in theory.

Each consumer enforces idempotency with a database `UNIQUE` constraint as the actual backstop —
`audit_log.event_id` and `notification_log (event_id, customer_id)` — exactly the same pattern
`TransferService`'s idempotency key already uses: catch the insert conflict, treat it as "already
done," move on. This is deliberately the same idiom repeated rather than a new one invented for
this phase.

## Consequence

An event can take up to the relay's poll interval (2 seconds) to reach Kafka after a transfer
completes — this is eventual, not immediate, consistency for anything downstream of the outbox,
which is the correct and expected tradeoff for this pattern. A consumer that is *not* idempotent
would be a latent bug under this design, not a hypothetical one: the audit and notification
consumers are built idempotent from the start specifically because "the event might arrive twice"
is a guarantee of this architecture, not an edge case bolted on afterward.
