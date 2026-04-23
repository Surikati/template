CREATE TABLE questionnaire (
    id                      UUID PRIMARY KEY,
    template_id             UUID NOT NULL,
    template_version_number INTEGER NOT NULL,
    name                    VARCHAR(500) NOT NULL,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT questionnaire_template_uq UNIQUE (template_id, template_version_number)
);

CREATE TABLE questionnaire_section (
    id                  UUID PRIMARY KEY,
    questionnaire_id    UUID NOT NULL REFERENCES questionnaire(id) ON DELETE CASCADE,
    ordinal             INTEGER NOT NULL,
    title               VARCHAR(500) NOT NULL,
    visibility_rule     TEXT
);

CREATE TABLE questionnaire_question (
    id                  UUID PRIMARY KEY,
    section_id          UUID NOT NULL REFERENCES questionnaire_section(id) ON DELETE CASCADE,
    ordinal             INTEGER NOT NULL,
    variable_path       VARCHAR(500) NOT NULL,
    label               VARCHAR(1000) NOT NULL,
    question_type       VARCHAR(30) NOT NULL,
    validation          JSONB,
    visibility_rule     TEXT,
    options             JSONB,
    CONSTRAINT question_type_ck CHECK (question_type IN
        ('TEXT','NUMBER','DATE','BOOLEAN','SELECT','MULTISELECT','GROUP'))
);

CREATE TABLE questionnaire_session (
    id                  UUID PRIMARY KEY,
    questionnaire_id    UUID NOT NULL REFERENCES questionnaire(id),
    state               VARCHAR(20) NOT NULL,
    started_by          UUID NOT NULL,
    started_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at        TIMESTAMPTZ,
    answers             JSONB NOT NULL DEFAULT '{}'::jsonb,
    current_section_id  UUID,
    CONSTRAINT session_state_ck CHECK (state IN ('IN_PROGRESS','COMPLETED','ABANDONED'))
);

CREATE INDEX idx_session_user ON questionnaire_session (started_by, state);

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
