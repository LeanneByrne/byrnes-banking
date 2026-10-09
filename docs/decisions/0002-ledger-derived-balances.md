# 0002 — Ledger-derived balances, not a stored column

**Status:** Accepted

## Context

A balance needs to be readable cheaply and needs to always be correct. The obvious approach —
a `balance_cents` column on `accounts`, updated whenever a transfer posts — is faster to read
but creates a second source of truth that has to be kept in lockstep with every ledger write,
forever, with no exceptions.

## Decision

`accounts` has no balance column. `AccountService.getBalanceCents` computes the balance on read
by summing every `ledger_entries` row for that account — credits minus debits.

## Why not a stored column

A stored balance is a cached derivative of the ledger that can drift from it: a missed update,
a bug in one code path that updates the ledger but forgets the column, a direct DB edit during
an incident, a migration that touches one table and not the other. The moment that drift
happens, there are two different answers to "what is this account's balance," and the ledger —
the actual history of every transaction — stops being authoritative. For a system whose entire
point is an auditable transaction history, that's not an acceptable failure mode.

Deriving the balance makes this class of bug structurally impossible: there is only ever one
thing that defines the balance, and it's the same thing an auditor would compute by hand from
the transaction history.

## Consequence

Reading a balance costs a query (`sumBalanceCentsByAccountId`) instead of a column read. For
this system's scale and demo purpose, that cost is negligible and the correctness guarantee is
worth it unconditionally. At a scale where this read becomes a real bottleneck, the standard
fix is a periodically-refreshed read-through cache or materialized view that is explicitly
understood to be a cache — rebuildable from the ledger at any time — never a second
independently-written source of truth.
