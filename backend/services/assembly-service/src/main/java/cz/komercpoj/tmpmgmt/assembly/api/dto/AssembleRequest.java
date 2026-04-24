package cz.komercpoj.tmpmgmt.assembly.api.dto;

import cz.komercpoj.tmpmgmt.assembly.domain.OutputFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.Map;
import java.util.UUID;

public record AssembleRequest(
        @NotNull UUID templateId,
        @Positive int templateVersionNumber,
        @NotNull Map<String, Object> data,
        OutputFormat format) {

    public OutputFormat formatOrDefault() {
        return format != null ? format : OutputFormat.DOCX;
    }
}
