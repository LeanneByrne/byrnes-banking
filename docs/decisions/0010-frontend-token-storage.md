# 0010 — Frontend JWT storage: in-memory only

**Status:** Accepted

## Context

The React frontend needs to hold the JWT it receives from `/api/auth/login` somewhere, and attach
it to every subsequent request. The common options are `localStorage`, `sessionStorage`, or
keeping it only in client-side application state (here, a React context).

## Decision

The token lives only in a React context (`AuthContext`), backed by `useState`. It is never
written to `localStorage` or `sessionStorage`. A page reload clears the session; logging back in
is one click via the demo-login buttons.

## Why not `localStorage`/`sessionStorage`

Anything in `localStorage` or `sessionStorage` is readable by **any JavaScript running on the
page**, including code from a vulnerable dependency pulled in transitively, not just code the
project's own developers wrote. If any script on the page is ever compromised (a supply-chain
attack on an npm package, for instance), a token sitting in browser storage is trivially
exfiltrated — it's just a synchronous property read. A token that exists only in one React
component tree's in-memory state has a meaningfully smaller blast radius: it's not persisted
anywhere a later, unrelated vulnerability could quietly go looking for it, and it doesn't survive
into a new tab or a new page load for an attacker's script to find later.

This isn't a complete defense against XSS — a sufficiently capable injected script running
*during* an active session could still read the token out of memory via React internals or by
intercepting the fetch calls themselves. The real fix for that class of attack is preventing XSS
in the first place (output encoding, a strict Content Security Policy, dependency hygiene). What
in-memory storage specifically buys is removing the *passive, persistent* exposure: a token that
isn't sitting in a storage API is not a thing a later-loaded, unrelated malicious script can find
by just checking `localStorage` on a whim.

## Consequence

Refreshing the page or closing the tab ends the session, with no "remember me" behavior. For a
demo app where logging back in is a single click on a pre-filled demo button, this cost is
negligible. A production system making this same choice would pair it with a short-lived
access token plus a `HttpOnly` refresh-token cookie (invisible to JavaScript entirely) to avoid
forcing a real user to re-authenticate on every reload — out of scope here, since this project
has no refresh-token mechanism at all (see [ADR 0007](0007-jwt-auth-design.md)).
