package cz.komercpoj.tmpmgmt.admin.api.dto;

public record RoleResponse(String code, String displayName, String description) {}

// Separate record for assigned role with metadata
