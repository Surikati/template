package cz.komercpoj.tmpmgmt.admin.api.dto;

import java.time.Instant;
import java.util.UUID;

public record UserRoleResponse(UUID userId, String roleCode, Instant grantedAt, UUID grantedBy) {}
