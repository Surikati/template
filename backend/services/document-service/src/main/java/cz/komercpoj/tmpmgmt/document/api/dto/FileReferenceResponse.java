package cz.komercpoj.tmpmgmt.document.api.dto;

import cz.komercpoj.tmpmgmt.document.domain.FileFormat;
import java.time.Instant;
import java.util.UUID;

public record FileReferenceResponse(
    UUID id,
    FileFormat format,
    long sizeBytes,
    String sha256,
    String downloadUrl,
    Instant createdAt) {}
