package cz.komercpoj.tmpmgmt.document.api.dto;

import tools.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.UUID;

public record UploadDocumentRequest(
    @NotNull UUID templateId,
    @Positive int templateVersionNumber,
    @NotNull UUID assemblyJobId,
    @NotNull JsonNode inputDataSnapshot,
    @NotEmpty @Valid List<FileInputDto> files) {}
