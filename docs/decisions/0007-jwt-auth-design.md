# 0007 — Stateless JWT auth, no `UserDetailsService`

**Status:** Accepted

## Context

The API needs authentication (who are you) and authorization (what are you allowed to touch),
including both role-based access (only ADMIN can list all customers) and per-resource ownership
checks (a customer can only read their own accounts, not another customer's by guessing an ID).

## Decision

Authentication is a stateless JWT issued by `AuthController` on register/login, verified on
every subsequent request by a custom `JwtAuthenticationFilter`. There is no
`UserDetailsService` and no `AuthenticationManager` — the filter trusts the token's signed
claims (`customerId`, `role`) directly and sets them as a small `CurrentUser` record on the
Spring Security context, rather than using the token only to look up a session or re-fetching
the customer from the database on every request. Role checks (`hasRole("ADMIN")`) are enforced
by Spring Security's filter chain; ownership checks (does this specific account belong to this
specific customer) are enforced inside each service method, which takes the acting customer's
id and role as explicit parameters rather than reaching into `SecurityContextHolder` itself.

## Why no `UserDetailsService`

`UserDetailsService` and `AuthenticationManager` exist to support Spring Security's
username/password login machinery and session-based flows. This system never has a session to
manage — the JWT itself *is* the credential on every request, verified by signature and expiry
alone. Adding that machinery here would mean carrying ceremony built for a different
authentication model (one that re-authenticates against a user store on every request) without
using any of what it's actually for.

## Why ownership checks live in services, not controllers or `SecurityContextHolder` lookups

Taking the actor as an explicit method parameter (`getById(accountId, actingCustomerId,
actingRole)`) rather than having the service reach into `SecurityContextHolder` keeps "who is
asking" as the controller's concern and "is this access allowed" as the service's concern. It
also means ownership logic can be unit-tested with plain Mockito and no Spring Security test
scaffolding — a test can simply pass in different customer ids and assert the right exception.

## Consequence (accepted tradeoff)

A customer's token stays valid until it expires even if their account is disabled immediately
afterward — there is no per-request database check of `enabled`. Adding one would mean a DB hit
on every authenticated request, which defeats the performance reason for choosing stateless
JWTs in the first place. This is a deliberate, documented tradeoff bounded by the token's
expiration window (60 minutes), not an oversight.
