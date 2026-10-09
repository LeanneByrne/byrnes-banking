# 0003 — Double-entry invariant and immutable ledger entries

**Status:** Accepted

## Context

A transfer moves money from one account to another. The system needs a representation of that
movement which can never silently lose, duplicate, or fabricate money, and which stays a
truthful record even when a transfer fails partway or needs to be corrected later.

## Decision

Every completed transfer writes exactly one `DEBIT` row and one equal-amount `CREDIT` row to
`ledger_entries`, in the same database transaction as the transfer itself. `ledger_entries` has
no update or delete path anywhere in the application — not in the repository interface, not in
any service. Corrections are made by inserting new, reversing entries, never by editing or
removing a past one.

## Why double-entry specifically

Double-entry bookkeeping is a five-hundred-year-old technique for a reason: it makes "where did
this money come from and where did it go" a structural property of every record, not something
that has to be separately verified. Summing every debit and credit across the entire ledger
always nets to exactly zero if the invariant holds — money only ever moves, it never appears or
vanishes. That's a cheap, powerful sanity check that this project's concurrency test actually
runs: after firing 20 simultaneous transfer attempts, it sums the *entire* ledger table and
asserts it's still zero, not just that the one account being tested looks right.

## Why entries are immutable

If a ledger row can be edited after the fact, the ledger stops being evidence of what actually
happened and becomes evidence of whatever it currently says — which could be different from
what it said yesterday. A reversing entry (a new row that undoes a prior one) preserves the
complete, honest history: the mistake, and the correction, are both visible forever. This is
also the only model that matches how real financial systems and auditors expect corrections to
work — nothing is ever quietly erased.

## Consequence

There is no `POST /transfers/{id}/reverse` endpoint yet (planned, tracked as a later phase) —
today, reversing a transfer would mean posting a new transfer in the opposite direction. The
invariant (never update/delete a ledger row) is enforced from the very first migration, even
though the reversal *feature* on top of it hasn't been built yet, because retrofitting the
invariant onto a system that already allows mutation is far riskier than building the feature
onto an invariant that was never violated.
