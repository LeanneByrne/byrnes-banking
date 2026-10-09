# 0005 — Consistent-order account locking

**Status:** Accepted

## Context

A transfer has to lock both the source and destination account rows before checking balance and
writing the ledger, so no other transfer can read or modify either account in between (see
[ADR 0004](0004-idempotency-key-strategy.md) and the architecture doc for why the check and the
write have to happen under the same lock). With two accounts involved per transfer, there are
two rows to lock, acquired one after another — and the order in which they're acquired matters
more than it might look.

## Decision

`TransferService.transferWithLocks` always locks the two accounts **lowest-UUID-first**,
computed from `sourceId.compareTo(destinationId)`, regardless of which account is actually the
source and which is the destination for that particular transfer. Locks are acquired via
`SELECT ... FOR UPDATE` (`AccountRepository.findByIdForUpdate`).

## Why direction-independent order prevents deadlocks

Picture two transfers happening at the same moment between accounts A and B: one transfer sends
A → B, the other sends B → A. If each transfer locked its own source first, transfer 1 would
lock A and then wait for B, while transfer 2 locked B and then wait for A — each holding the
lock the other one needs, forever. That's a classic deadlock, and it would only show up under
real concurrent load, making it exactly the kind of bug that's painful to reproduce and
practically invisible on a single-user test.

Locking strictly by account ID instead of by transfer role (source/destination) removes the
cycle: both transfers now compete for the same first lock (whichever account has the lower ID),
so one of them simply waits for the other to finish — no two transfers can ever be holding a
lock the other one is waiting for.

## Consequence

The code has to carefully re-derive which of the two locked rows is actually the source and
which is the destination *after* locking them in ID order, since the lock-acquisition order and
the transfer's semantic direction are deliberately decoupled. This is a small amount of extra
bookkeeping in exchange for deadlock-freedom that holds regardless of transfer direction or
load pattern, which is the only version of this guarantee worth having.
