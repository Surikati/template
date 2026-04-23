package cz.komercpoj.tmpmgmt.assembly.api.dto;

import cz.komercpoj.tmpmgmt.assembly.domain.AssemblyState;
import java.time.Instant;
import java.util.UUID;

public record AssembleResponse(
        UUID jobId,
        AssemblyState state,
        String filename,
        /** DOCX bytes as Base64. MVP only — future will replace with MinIO-backed download URL. */
        String contentBase64,
        Instant completedAt) {}
