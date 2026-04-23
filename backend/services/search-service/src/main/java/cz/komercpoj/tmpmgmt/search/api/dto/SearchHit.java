package cz.komercpoj.tmpmgmt.search.api.dto;

import java.time.Instant;
import java.util.UUID;

public record SearchHit(
        UUID id,
        String type,      // "template" | "clause"
        String slug,
        String name,
        String description,
        String category,
        String status,
        Instant updatedAt,
        double score) {}
