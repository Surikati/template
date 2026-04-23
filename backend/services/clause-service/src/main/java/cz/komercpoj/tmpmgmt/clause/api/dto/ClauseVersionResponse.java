package cz.komercpoj.tmpmgmt.clause.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

public record ClauseVersionResponse(
        UUID id,
        UUID clauseId,
        int versionNumber,
        JsonNode content,
        String changeNote,
        Instant publishedAt,
        UUID publishedBy) {}
