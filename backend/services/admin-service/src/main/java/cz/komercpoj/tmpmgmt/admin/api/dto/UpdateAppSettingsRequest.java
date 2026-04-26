package cz.komercpoj.tmpmgmt.admin.api.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateAppSettingsRequest(
    @NotBlank String locale, @NotBlank String timezone, @NotBlank String currency) {}
