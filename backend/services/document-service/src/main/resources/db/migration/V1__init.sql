CREATE TABLE generated_document (
    id                      UUID PRIMARY KEY,
    template_id             UUID NOT NULL,
    template_version_number INTEGER NOT NULL,
    assembly_job_id         UUID NOT NULL,
    input_data_snapshot     JSONB NOT NULL,
    created_by              UUID NOT NULL,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_document_template ON generated_document (template_id, template_version_number);
CREATE INDEX idx_document_created_by ON generated_document (created_by, created_at DESC);

CREATE TABLE file_reference (
    id              UUID PRIMARY KEY,
    document_id     UUID NOT NULL REFERENCES generated_document(id) ON DELETE CASCADE,
    format          VARCHAR(10) NOT NULL,
    minio_key       VARCHAR(500) NOT NULL,
    size_bytes      BIGINT NOT NULL,
    sha256          CHAR(64) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT file_format_ck CHECK (format IN ('DOCX','PDF','HTML')),
    CONSTRAINT file_doc_format_uq UNIQUE (document_id, format)
);

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
