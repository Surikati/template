-- Template aggregate

CREATE TABLE template (
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
    CONSTRAINT template_status_ck CHECK (status IN ('ACTIVE', 'ARCHIVED'))
);

CREATE INDEX idx_template_status ON template (status);
CREATE INDEX idx_template_category ON template (category);

CREATE TABLE template_draft (
    template_id         UUID PRIMARY KEY REFERENCES template(id) ON DELETE CASCADE,
    content             JSONB NOT NULL,
    variables_schema    JSONB NOT NULL,
    last_edited_by      UUID NOT NULL,
    last_edited_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE template_version (
    id                  UUID PRIMARY KEY,
    template_id         UUID NOT NULL REFERENCES template(id) ON DELETE CASCADE,
    version_number      INTEGER NOT NULL,
    content             JSONB NOT NULL,
    variables_schema    JSONB NOT NULL,
    change_note         TEXT,
    published_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_by        UUID NOT NULL,
    CONSTRAINT template_version_uq UNIQUE (template_id, version_number)
);

-- Enforce immutability: reject UPDATE on template_version.
CREATE OR REPLACE FUNCTION reject_template_version_update() RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'template_version is immutable after publish (id=%)', OLD.id;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER template_version_immutable
    BEFORE UPDATE ON template_version
    FOR EACH ROW EXECUTE FUNCTION reject_template_version_update();

-- Clause references embedded in a published version (for impact analysis).
CREATE TABLE template_clause_ref (
    template_version_id UUID NOT NULL REFERENCES template_version(id) ON DELETE CASCADE,
    clause_id           UUID NOT NULL,
    clause_version      INTEGER NOT NULL,
    PRIMARY KEY (template_version_id, clause_id)
);

-- Outbox (canonical schema from outbox-starter)
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
