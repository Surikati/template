package cz.komercpoj.tmpmgmt.assembly.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.Map;
import java.util.UUID;

public record AssembleRequest(
        @NotNull UUID templateId,
        @Positive int templateVersionNumber,
        @NotNull Map<String, Object> data) {}
