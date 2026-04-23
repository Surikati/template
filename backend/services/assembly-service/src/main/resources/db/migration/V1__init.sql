CREATE TABLE assembly_job (
    id                      UUID PRIMARY KEY,
    template_id             UUID NOT NULL,
    template_version_number INTEGER NOT NULL,
    input_data              JSONB NOT NULL,
    requested_formats       TEXT[] NOT NULL,
    state                   VARCHAR(30) NOT NULL,
    result_document_id      UUID,
    error_code              VARCHAR(100),
    error_message           TEXT,
    requested_by            UUID NOT NULL,
    requested_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at            TIMESTAMPTZ,
    CONSTRAINT job_state_ck CHECK (state IN
        ('PENDING','RESOLVING_CLAUSES','RENDERING','COMPLETED','FAILED'))
);

CREATE INDEX idx_assembly_job_state ON assembly_job (state, requested_at);

CREATE TABLE outbox_event (
    event_id        UUID PRIMARY KEY,
    aggregate_type  VARCHAR(100) NOT NULL,
    aggregate_id    VARCHAR(100) NOT NULL,
    event_type      VARCHAR(100) NOT NULL,
    payload         JSONB NOT NULL,
    occurred_at     TIMESTAMPTZ NOT NULL,
    published_at    TIMESTAMPTZ
);

CREATE INDEX idx_outbox_unpublished
    ON outbox_event (occurred_at)
    WHERE published_at IS NULL;
