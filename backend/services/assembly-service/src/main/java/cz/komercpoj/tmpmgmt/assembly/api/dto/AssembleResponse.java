package cz.komercpoj.tmpmgmt.assembly.api.dto;

import cz.komercpoj.tmpmgmt.assembly.domain.AssemblyState;
import cz.komercpoj.tmpmgmt.assembly.domain.OutputFormat;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AssembleResponse(
    UUID jobId,
    AssemblyState state,
    UUID documentId,
    List<AssembledFile> files,
    Instant completedAt) {

  public record AssembledFile(OutputFormat format, String filename, String downloadUrl) {}
}
