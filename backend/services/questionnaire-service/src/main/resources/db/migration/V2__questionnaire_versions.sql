-- Immutable snapshot of a questionnaire structure. The mutable "questionnaire" table
-- (with its section/question child tables) functions as the editor draft; publishing
-- freezes the current state into a JSONB blob here. Sessions started against a
-- specific version load the snapshot, decoupled from any subsequent draft edits.
CREATE TABLE questionnaire_version (
    id                          UUID PRIMARY KEY,
    questionnaire_id            UUID NOT NULL REFERENCES questionnaire(id) ON DELETE CASCADE,
    version_number              INTEGER NOT NULL,
    name_snapshot               VARCHAR(500) NOT NULL,
    -- Full sections+questions structure as JSON, shape mirroring QuestionnaireResponse.
    structure_snapshot          JSONB NOT NULL,
    published_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_by                UUID NOT NULL,
    CONSTRAINT questionnaire_version_uq UNIQUE (questionnaire_id, version_number)
);

CREATE INDEX idx_questionnaire_version_questionnaire
    ON questionnaire_version (questionnaire_id, version_number DESC);
