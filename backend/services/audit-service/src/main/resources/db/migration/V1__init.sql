-- Append-only audit log, partitioned by month.
CREATE TABLE audit_event (
    event_id        UUID NOT NULL,
    occurred_at     TIMESTAMPTZ NOT NULL,
    actor_user_id   UUID,
    aggregate_type  VARCHAR(100) NOT NULL,
    aggregate_id    VARCHAR(100) NOT NULL,
    event_type      VARCHAR(100) NOT NULL,
    correlation_id  VARCHAR(100),
    payload         JSONB NOT NULL,
    PRIMARY KEY (event_id, occurred_at)
) PARTITION BY RANGE (occurred_at);

-- Revoke any UPDATE/DELETE at the role level when deploying.
CREATE INDEX idx_audit_aggregate
    ON audit_event (aggregate_type, aggregate_id, occurred_at DESC);
CREATE INDEX idx_audit_actor
    ON audit_event (actor_user_id, occurred_at DESC);

-- Create initial monthly partitions (scheduler/cron adds future ones).
CREATE TABLE audit_event_2026_04 PARTITION OF audit_event
    FOR VALUES FROM ('2026-04-01') TO ('2026-05-01');
CREATE TABLE audit_event_2026_05 PARTITION OF audit_event
    FOR VALUES FROM ('2026-05-01') TO ('2026-06-01');
CREATE TABLE audit_event_2026_06 PARTITION OF audit_event
    FOR VALUES FROM ('2026-06-01') TO ('2026-07-01');
