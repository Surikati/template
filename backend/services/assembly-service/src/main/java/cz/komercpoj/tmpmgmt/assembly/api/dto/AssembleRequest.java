package cz.komercpoj.tmpmgmt.assembly.api.dto;

import cz.komercpoj.tmpmgmt.assembly.domain.OutputFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * {@code formats} is a list because a single contract is often shipped as both an editable DOCX and
 * an archival PDF — generating both in one job avoids re-resolving clauses + re-rendering. Defaults
 * to {@code [DOCX]} when omitted, preserving the prior single-format behavior.
 */
public record AssembleRequest(
    @NotNull UUID templateId,
    @Positive int templateVersionNumber,
    @NotNull Map<String, Object> data,
    List<OutputFormat> formats) {

  public List<OutputFormat> formatsOrDefault() {
    return formats == null || formats.isEmpty() ? List.of(OutputFormat.DOCX) : formats;
  }
}
