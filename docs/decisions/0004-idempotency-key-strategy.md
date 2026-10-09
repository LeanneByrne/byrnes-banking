# 0004 — Idempotency key strategy for transfers

**Status:** Accepted

## Context

Clients retry requests — a mobile app loses signal after sending a transfer but before seeing
the response, a load balancer times out and the caller retries, a user double-taps a button.
Without protection, a retried transfer request would move money twice for what the user
experienced as a single action.

## Decision

`POST /api/transfers` requires a client-supplied `Idempotency-Key` header. The first request
seen with a given key executes normally. Any later request with the same key returns the
original result:
- same key + same payload (source, destination, amount) → the original response, no
  re-execution, not even a balance re-check
- same key + a **different** payload → `409 Conflict` (`DuplicateIdempotencyKeyException`) —
  this is a client bug (key reuse across genuinely different requests), not a retry, and must
  never be silently treated as one
- same key, and the original attempt failed (e.g. insufficient funds) → re-throws the exact
  same error every time, rather than silently re-evaluating against a balance that may have
  since changed

Two enforcement layers back this up: an application-level lookup (`findByIdempotencyKey`)
handles the common retry case cheaply, and the `idempotency_key` column's database `UNIQUE`
constraint is the backstop for the rare case where two first-attempts with the same key race
each other past the lookup simultaneously — whichever insert loses becomes a replay of the
winner instead of an error.

## Why the key is client-supplied, not server-generated

The whole point is that the *client* can safely retry a request it's unsure succeeded. If the
server generated the key, a client that lost the response would have no way to supply the same
key on retry, and the protection wouldn't cover the actual failure mode it exists for.

## Consequence

Clients are responsible for generating a sufficiently unique key per logical transfer attempt
(a UUID per user action, not per HTTP call) and must not reuse a key across unrelated transfers.
This pushes a small amount of correctness responsibility onto the client, which is the standard,
correct tradeoff for idempotency keys industry-wide (Stripe, for example, uses the same model).
