# 0006 — Flyway owns the schema, Hibernate never mutates it

**Status:** Accepted

## Context

Spring/Hibernate can manage schema automatically (`ddl-auto: update` or `create`), which is
convenient in a tutorial but risky in anything resembling a real system: Hibernate's inferred
DDL is driven by whatever the entity classes currently look like, can differ across Hibernate
versions, and gives no reviewable, ordered history of how the schema got to its current shape.

## Decision

Flyway is the single owner of the database schema. Every schema change is a numbered,
version-controlled SQL migration (`V1__create_schema.sql`, `V2__seed_demo_data.sql`, ...).
`spring.jpa.hibernate.ddl-auto` is set to `validate` — Hibernate checks that the entity mappings
match the actual schema at startup and fails fast if they don't, but it never issues a single
DDL statement of its own.

## Why this matters for a banking-style system

Schema changes to tables holding financial data need to be deliberate, reviewable, and
reproducible identically across every environment — a developer's laptop, CI, and (eventually)
production all have to apply the exact same sequence of changes in the exact same order. Raw
SQL migrations under version control give that directly: the migration files *are* the schema's
change history, reviewable in a pull request like any other code change, with no "well it
worked on my machine because Hibernate inferred something slightly different" class of bug.

`ddl-auto: validate` closes the other half of the gap: if an entity class and the actual schema
disagree (a column renamed in Java but not in a migration, say), the application fails to start
with a clear error instead of Hibernate silently reconciling the difference by issuing DDL no
one reviewed.

## Consequence

Every schema change requires writing an explicit, immutable SQL migration file, which is more
upfront effort than letting Hibernate infer changes automatically. In exchange, the schema's
entire history is readable in version control, identical across every environment that runs the
migrations, and never silently altered by the application layer at runtime.
