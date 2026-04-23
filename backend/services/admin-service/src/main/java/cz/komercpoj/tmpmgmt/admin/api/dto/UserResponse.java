package cz.komercpoj.tmpmgmt.admin.api.dto;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String keycloakSubject,
        String username,
        String email,
        String displayName,
        boolean active,
        Instant createdAt,
        Instant lastSyncedAt) {}
