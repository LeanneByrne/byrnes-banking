# 0001 — Money as integer cents

**Status:** Accepted

## Context

Every amount in the system — account balances, transfer amounts, ledger entries — needs exact
arithmetic. Addition, subtraction, and comparison all have to be exact, with no rounding and no
representation error, because this is money: "close enough" is a bug, not a rounding footnote.

## Decision

Store and compute every monetary amount as a `long` number of cents (`amountCents`). Never use
`double`, `float`, or `BigDecimal` anywhere in the money-handling path.

## Why not `double`/`float`

Binary floating point cannot represent most decimal fractions exactly. `0.1 + 0.2` does not
equal `0.3` in IEEE 754 double-precision arithmetic — it's off by a tiny amount that compounds
over enough operations. For a banking system that sums ledger entries to derive a balance, that
compounding isn't a hypothetical edge case; it's a guaranteed eventual discrepancy between what
the ledger says happened and what the balance claims is true.

## Why not `BigDecimal`

`BigDecimal` is exact, but it brings its own hazard: equality, scale, and rounding mode all
have to be handled explicitly and consistently everywhere the type is touched, and getting any
one of those wrong (e.g. comparing `1.10` and `1.1` with `equals()` instead of `compareTo()`)
reintroduces subtle bugs. It also doesn't map as directly onto a database `BIGINT` column, and
adds API surface (rounding, scale, `MathContext`) that this system has no actual use for.

## Consequence

Every amount is a whole number of the smallest currency unit. `amount_cents BIGINT` columns,
`long amountCents` fields throughout, and all arithmetic is plain integer arithmetic — exact by
construction, no rounding mode to get wrong. The tradeoff is that displaying an amount to a
human (`"$12.50"`) requires an explicit cents-to-display-string conversion at the presentation
boundary; that conversion happens in exactly one place rather than money being stored in a
display-friendly but arithmetically unsafe format everywhere.
