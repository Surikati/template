-- Local projection of Keycloak users (synced via SCIM/events).
CREATE TABLE app_user (
    id                  UUID PRIMARY KEY,
    keycloak_subject    VARCHAR(100) NOT NULL UNIQUE,
    username            VARCHAR(200) NOT NULL,
    email               VARCHAR(320) NOT NULL,
    display_name        VARCHAR(500),
    active              BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_synced_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE role_definition (
    code                VARCHAR(50) PRIMARY KEY,
    display_name        VARCHAR(200) NOT NULL,
    description         TEXT
);

INSERT INTO role_definition (code, display_name, description) VALUES
    ('ADMIN',           'Administrátor',          'Full system administration.'),
    ('TEMPLATE_EDITOR', 'Editor šablon',          'Can create and publish templates.'),
    ('CLAUSE_EDITOR',   'Editor doložek',         'Can create and publish clauses.'),
    ('USER',            'Uživatel',               'Can generate documents from existing templates.');

CREATE TABLE user_role (
    user_id     UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    role_code   VARCHAR(50) NOT NULL REFERENCES role_definition(code),
    granted_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    granted_by  UUID,
    PRIMARY KEY (user_id, role_code)
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
