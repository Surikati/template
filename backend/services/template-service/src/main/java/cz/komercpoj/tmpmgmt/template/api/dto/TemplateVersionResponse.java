package cz.komercpoj.tmpmgmt.template.api.dto;

import tools.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

public record TemplateVersionResponse(
    UUID id,
    UUID templateId,
    int versionNumber,
    JsonNode content,
    JsonNode variablesSchema,
    String changeNote,
    Instant publishedAt,
    UUID publishedBy) {}
