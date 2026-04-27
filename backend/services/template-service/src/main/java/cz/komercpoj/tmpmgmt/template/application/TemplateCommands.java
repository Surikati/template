package cz.komercpoj.tmpmgmt.template.application;

import tools.jackson.databind.JsonNode;
import java.util.List;
import java.util.UUID;

/** Intent records passed into {@link TemplateService}. Keep controller DTOs out of the service. */
public final class TemplateCommands {

  private TemplateCommands() {}

  public record CreateTemplate(
      String slug, String name, String description, String category, UUID ownerUserId) {}

  public record UpdateMetadata(
      UUID templateId,
      String name,
      String description,
      String category,
      List<String> tags,
      UUID actorUserId) {}

  public record SaveDraft(
      UUID templateId, JsonNode content, JsonNode variablesSchema, UUID editorUserId) {}

  public record PublishVersion(UUID templateId, String changeNote, UUID publishedBy) {}

  public record Archive(UUID templateId, UUID actorUserId) {}

  public record ImportBundle(
      String slug,
      String name,
      String description,
      String category,
      List<String> tags,
      JsonNode draftContent,
      JsonNode draftSchema,
      List<ImportedVersion> versions,
      UUID importedBy) {}

  public record ImportedVersion(
      int versionNumber, JsonNode content, JsonNode variablesSchema, String changeNote) {}
}
