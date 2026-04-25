package cz.komercpoj.tmpmgmt.template.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "template_draft")
@Getter
@Setter
@NoArgsConstructor
public class TemplateDraftEntity {

  @Id
  @Column(name = "template_id")
  private UUID templateId;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false, columnDefinition = "jsonb")
  private JsonNode content;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "variables_schema", nullable = false, columnDefinition = "jsonb")
  private JsonNode variablesSchema;

  @Column(name = "last_edited_by", nullable = false)
  private UUID lastEditedBy;

  @Column(name = "last_edited_at", nullable = false)
  private Instant lastEditedAt;

  public static TemplateDraftEntity empty(
      UUID templateId, JsonNode emptyContent, JsonNode emptySchema, UUID editorUserId) {
    TemplateDraftEntity e = new TemplateDraftEntity();
    e.templateId = templateId;
    e.content = emptyContent;
    e.variablesSchema = emptySchema;
    e.lastEditedBy = editorUserId;
    e.lastEditedAt = Instant.now();
    return e;
  }

  public void replace(JsonNode newContent, JsonNode newSchema, UUID editorUserId) {
    this.content = newContent;
    this.variablesSchema = newSchema;
    this.lastEditedBy = editorUserId;
    this.lastEditedAt = Instant.now();
  }
}
