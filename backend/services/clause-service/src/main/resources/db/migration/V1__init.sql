CREATE TABLE clause (
    id              UUID PRIMARY KEY,
    slug            VARCHAR(200) NOT NULL UNIQUE,
    name            VARCHAR(500) NOT NULL,
    description     TEXT,
    category        VARCHAR(100),
    tags            TEXT[] NOT NULL DEFAULT '{}',
    status          VARCHAR(20) NOT NULL,
    owner_user_id   UUID NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT clause_status_ck CHECK (status IN ('ACTIVE', 'ARCHIVED'))
);

CREATE INDEX idx_clause_category ON clause (category);
CREATE INDEX idx_clause_status ON clause (status);

CREATE TABLE clause_version (
    id                  UUID PRIMARY KEY,
    clause_id           UUID NOT NULL REFERENCES clause(id) ON DELETE CASCADE,
    version_number      INTEGER NOT NULL,
    content             JSONB NOT NULL,
    change_note         TEXT,
    published_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_by        UUID NOT NULL,
    CONSTRAINT clause_version_uq UNIQUE (clause_id, version_number)
);

CREATE OR REPLACE FUNCTION reject_clause_version_update() RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'clause_version is immutable after publish (id=%)', OLD.id;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER clause_version_immutable
    BEFORE UPDATE ON clause_version
    FOR EACH ROW EXECUTE FUNCTION reject_clause_version_update();

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
