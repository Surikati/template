package cz.komercpoj.tmpmgmt.assembly.api.dto;

import cz.komercpoj.tmpmgmt.assembly.domain.AssemblyState;
import java.time.Instant;
import java.util.UUID;

public record AssembleResponse(
        UUID jobId,
        AssemblyState state,
        UUID documentId,
        String filename,
        String downloadUrl,
        Instant completedAt) {}
