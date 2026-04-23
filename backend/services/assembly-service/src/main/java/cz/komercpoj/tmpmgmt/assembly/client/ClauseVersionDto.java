package cz.komercpoj.tmpmgmt.assembly.client;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

/** Projection of clause-service's GET /clauses/{id}/versions/{v} response. */
public record ClauseVersionDto(
        UUID id,
        UUID clauseId,
        int versionNumber,
        JsonNode content,
        String changeNote,
        Instant publishedAt,
        UUID publishedBy) {}
