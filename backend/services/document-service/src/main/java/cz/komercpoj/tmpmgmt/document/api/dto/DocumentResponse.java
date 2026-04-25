package cz.komercpoj.tmpmgmt.document.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DocumentResponse(
    UUID id,
    UUID templateId,
    int templateVersionNumber,
    UUID assemblyJobId,
    UUID createdBy,
    Instant createdAt,
    List<FileReferenceResponse> files) {}
