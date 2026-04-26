package cz.komercpoj.tmpmgmt.admin.api.dto;

import java.time.Instant;
import java.util.UUID;

public record AppSettingsResponse(
    String locale, String timezone, String currency, Instant updatedAt, UUID updatedBy) {}
