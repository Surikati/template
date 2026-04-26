package cz.komercpoj.tmpmgmt.template.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;

/**
 * Self-contained JSON bundle for moving a template between environments. Includes the metadata,
 * every immutable version snapshot, and the editor draft. {@code schemaVersion} is bumped only on
 * breaking changes so older exports remain importable.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TemplateBundle(
    int schemaVersion, Instant exportedAt, Metadata template, Draft draft, List<Version> versions) {

  public static final int CURRENT_SCHEMA_VERSION = 1;

  public record Metadata(
      @NotBlank String slug,
      @NotBlank String name,
      String description,
      String category,
      List<String> tags) {}

  public record Draft(@NotNull JsonNode content, @NotNull JsonNode variablesSchema) {}

  public record Version(
      int versionNumber,
      @NotNull JsonNode content,
      @NotNull JsonNode variablesSchema,
      String changeNote,
      Instant publishedAt) {}
}
