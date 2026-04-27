package cz.komercpoj.tmpmgmt.questionnaire.api.dto;

import tools.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

public record QuestionnaireVersionResponse(
    UUID id,
    UUID questionnaireId,
    int versionNumber,
    String name,
    /** Mirrors the {@code sections} field shape of {@link QuestionnaireResponse}. */
    JsonNode structure,
    Instant publishedAt,
    UUID publishedBy) {}
