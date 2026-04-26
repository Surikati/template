-- Application-level locale defaults. Single-tenant: a single row enforced via
-- a primary key fixed at 1. Reads are cheap; writes are admin-only.
CREATE TABLE app_settings (
    id              SMALLINT PRIMARY KEY DEFAULT 1 CHECK (id = 1),
    locale          VARCHAR(20)    NOT NULL,
    timezone        VARCHAR(50)    NOT NULL,
    currency        CHAR(3)        NOT NULL,
    updated_at      TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_by      UUID
);

-- Seed with the same defaults rendering-service hard-codes today, so existing
-- behaviour is unchanged on first deploy.
INSERT INTO app_settings (id, locale, timezone, currency, updated_at)
    VALUES (1, 'cs-CZ', 'Europe/Prague', 'CZK', now());
