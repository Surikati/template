package cz.komercpoj.tmpmgmt.assembly.client;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

/** Projection of the field shape returned by template-service GET /versions/{v}. */
public record TemplateVersionDto(
        UUID id,
        UUID templateId,
        int versionNumber,
        JsonNode content,
        JsonNode variablesSchema,
        String changeNote,
        Instant publishedAt,
        UUID publishedBy) {}
