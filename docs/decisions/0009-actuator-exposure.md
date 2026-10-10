# 0009 — Actuator exposure: health and metrics scraping stay public, everything else requires ADMIN

**Status:** Accepted

## Context

Spring Boot Actuator exposes operational endpoints under `/actuator/**` — health status, raw
metrics, environment info. Some of these need to be reachable by infrastructure that can't carry
a bearer token (a load balancer's health probe, Prometheus's scraper); others (a browsable
metrics explorer, environment details) are a human-readable surface that reveals internal
operational detail about the running system.

## Decision

`/actuator/health/**` and `/actuator/prometheus` are `permitAll()` in `SecurityConfig`. Every
other Actuator endpoint requires the `ADMIN` role, same as the other operational surface
(`GET /api/customers`) already does.

## Why these two specifically stay open

Both are machine-to-machine callers that structurally cannot authenticate the way a human client
does:

- A load balancer or orchestrator's health probe needs a simple, unauthenticated GET to decide
  whether to route traffic to this instance or restart it. Gating that behind a token the prober
  doesn't carry would make health checks themselves fail, which is worse than not having them.
- Prometheus's scraper polls `/actuator/prometheus` on a fixed schedule with no mechanism to
  carry a short-lived JWT (and minting one for it would mean either a non-expiring service-account
  token — a separate auth mechanism this project doesn't otherwise have — or re-authenticating
  the scraper every 60 minutes, which defeats the point of a scrape endpoint).

Both are accepted as public specifically *because* they're only reachable over the
docker-compose-internal network in this setup, not the public internet — the real-world
equivalent is a metrics/management endpoint bound to an internal-only port or network, not
actually exposed to arbitrary callers. A production deployment would enforce that boundary at
the network layer (a private subnet, a sidecar, a firewall rule) rather than at the application
layer; this project doesn't have that infrastructure, so the tradeoff is noted explicitly here
instead of silently assumed.

## Why everything else stays ADMIN-gated

`/actuator/metrics`, `/actuator/env`, and similar endpoints are a human-browsable window into
the running system's internals — request counts, JVM memory, configuration. That's exactly the
kind of information an attacker would want during reconnaissance, and exactly the kind of detail
that should require being a trusted operator to see, consistent with the project's existing rule
that reading another customer's account or listing every customer both require authorization,
not just authentication.

## Consequence

Prometheus's scrape config needs no credentials at all for this project, which is simpler than a
real production setup would allow permanently but is accurate to what this project's
infrastructure can support. If this ever moved to a real deployment, the fix would be a network
boundary (Prometheus and the app on a private network Grafana/the public internet can't reach
directly) rather than loosening or tightening anything in `SecurityConfig` itself.
