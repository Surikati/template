package cz.komercpoj.tmpmgmt.template.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

/**
 * Side-by-side comparison of two published template versions. Both snapshots are returned in full
 * (content + variables schema) so the FE can render them with the existing TipTap renderer; the
 * {@code summary} flags let callers cheaply check whether anything changed without diffing the
 * blobs themselves.
 */
public record TemplateVersionDiffResponse(
    UUID templateId, VersionSnapshot from, VersionSnapshot to, DiffSummary summary) {

  public record VersionSnapshot(
      int versionNumber,
      JsonNode content,
      JsonNode variablesSchema,
      String changeNote,
      Instant publishedAt,
      UUID publishedBy) {}

  public record DiffSummary(boolean contentChanged, boolean variablesSchemaChanged) {}
}
