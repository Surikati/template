package cz.komercpoj.tmpmgmt.clause.application;

import tools.jackson.databind.JsonNode;
import java.util.List;
import java.util.UUID;

public final class ClauseCommands {

  private ClauseCommands() {}

  public record CreateClause(
      String slug, String name, String description, String category, UUID ownerUserId) {}

  public record UpdateMetadata(
      UUID clauseId,
      String name,
      String description,
      String category,
      List<String> tags,
      UUID actorUserId) {}

  public record PublishVersion(
      UUID clauseId, JsonNode content, String changeNote, UUID publishedBy) {}

  public record Archive(UUID clauseId, UUID actorUserId) {}
}
