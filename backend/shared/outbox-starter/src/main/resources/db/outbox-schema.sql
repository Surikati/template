-- Canonical outbox schema. Each service copies this DDL into its own Flyway migration,
-- because the outbox table belongs to that service's database.

CREATE TABLE IF NOT EXISTS outbox_event (
    event_id        UUID PRIMARY KEY,
    aggregate_type  VARCHAR(100) NOT NULL,
    aggregate_id    VARCHAR(100) NOT NULL,
    event_type      VARCHAR(100) NOT NULL,
    payload         JSONB NOT NULL,
    occurred_at     TIMESTAMPTZ NOT NULL,
    published_at    TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_outbox_unpublished
    ON outbox_event (occurred_at)
    WHERE published_at IS NULL;
