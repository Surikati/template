package cz.komercpoj.tmpmgmt.clause.api.dto;

import cz.komercpoj.tmpmgmt.clause.domain.ClauseStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ClauseResponse(
    UUID id,
    String slug,
    String name,
    String description,
    String category,
    List<String> tags,
    ClauseStatus status,
    UUID ownerUserId,
    Instant createdAt,
    Instant updatedAt) {}
