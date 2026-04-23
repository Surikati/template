package cz.komercpoj.tmpmgmt.template.api.dto;

import cz.komercpoj.tmpmgmt.template.domain.TemplateStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TemplateResponse(
        UUID id,
        String slug,
        String name,
        String description,
        String category,
        List<String> tags,
        TemplateStatus status,
        UUID ownerUserId,
        Instant createdAt,
        Instant updatedAt) {}
