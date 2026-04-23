package cz.komercpoj.tmpmgmt.search.domain;

import java.time.Instant;
import java.util.UUID;

/** Flat projection stored in OpenSearch for both templates and clauses. */
public record SearchableDocument(
        UUID id,
        String slug,
        String name,
        String description,
        String category,
        String status,
        Instant updatedAt) {}
