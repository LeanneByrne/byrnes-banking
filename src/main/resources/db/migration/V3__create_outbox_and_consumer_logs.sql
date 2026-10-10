-- Transactional outbox: a row here is written in the SAME database
-- transaction as the business event it describes (e.g. a completed
-- transfer), so the two can never disagree -- either both commit or
-- neither does. A separate relay process publishes unpublished rows to
-- Kafka and stamps published_at, independent of this table's writers.
CREATE TABLE outbox_events (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type    VARCHAR(100) NOT NULL,
    payload       JSONB NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at  TIMESTAMPTZ
);

-- Partial index: the relay only ever queries for unpublished rows, and
-- published ones accumulate forever, so indexing just the unpublished
-- subset keeps that query cheap regardless of table size.
CREATE INDEX idx_outbox_events_unpublished ON outbox_events (created_at) WHERE published_at IS NULL;

-- The audit consumer's idempotency backstop: the outbox's at-least-once
-- delivery means the same event can arrive twice, and this UNIQUE
-- constraint on event_id is what turns a duplicate delivery into a no-op
-- insert conflict instead of a duplicate row -- the same pattern transfers
-- already use for idempotency keys.
CREATE TABLE audit_log (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id    UUID NOT NULL UNIQUE,
    event_type  VARCHAR(100) NOT NULL,
    payload     JSONB NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- One event can notify more than one customer (a transfer has both a
-- source and a destination owner), so the dedup key is the pair, not
-- event_id alone.
CREATE TABLE notification_log (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id    UUID NOT NULL,
    customer_id UUID NOT NULL,
    message     VARCHAR(500) NOT NULL,
    sent_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_notification_log_event_customer UNIQUE (event_id, customer_id)
);
