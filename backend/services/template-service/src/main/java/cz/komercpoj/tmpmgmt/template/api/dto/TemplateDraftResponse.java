package cz.komercpoj.tmpmgmt.template.api.dto;

import tools.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

public record TemplateDraftResponse(
    UUID templateId,
    JsonNode content,
    JsonNode variablesSchema,
    UUID lastEditedBy,
    Instant lastEditedAt) {}
