package cz.komercpoj.tmpmgmt.template.persistence;

import tools.jackson.databind.JsonNode;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "template_draft")
@Getter
@Setter
@NoArgsConstructor
public class TemplateDraftEntity implements Persistable<UUID> {

  @Id
  @Column(name = "template_id")
  private UUID templateId;

  /** See {@link TemplateEntity#isNew} — same Persistable rationale. */
  @Transient private boolean isNew = true;

  @Override
  public UUID getId() {
    return templateId;
  }

  @Override
  public boolean isNew() {
    return isNew;
  }

  @PostPersist
  @PostLoad
  void markNotNew() {
    this.isNew = false;
  }

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
