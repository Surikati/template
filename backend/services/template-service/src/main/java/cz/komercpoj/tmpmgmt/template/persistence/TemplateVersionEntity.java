package cz.komercpoj.tmpmgmt.template.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

/**
 * Published, immutable snapshot of a template. Enforced both by this {@link Immutable} annotation
 * (Hibernate-side guard) and by the {@code reject_template_version_update} DB trigger (defense in
 * depth). Any UPDATE will be rejected by Postgres regardless of ORM state.
 */
@Entity
@Table(name = "template_version")
@Immutable
@Getter
@NoArgsConstructor
public class TemplateVersionEntity implements Persistable<UUID> {

  @Id private UUID id;

  /**
   * Forces {@code save()} through {@code persist()} so the insert avoids a redundant {@code SELECT
   * WHERE id=?} that {@code merge()} routing would emit.
   */
  @Transient private boolean isNew = true;

  @Override
  public boolean isNew() {
    return isNew;
  }

  @PostPersist
  @PostLoad
  void markNotNew() {
    this.isNew = false;
  }

  @Column(name = "template_id", nullable = false)
  private UUID templateId;

  @Column(name = "version_number", nullable = false)
  private int versionNumber;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false, columnDefinition = "jsonb")
  private JsonNode content;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "variables_schema", nullable = false, columnDefinition = "jsonb")
  private JsonNode variablesSchema;

  @Column(name = "change_note", columnDefinition = "TEXT")
  private String changeNote;

  @Column(name = "published_at", nullable = false)
  private Instant publishedAt;

  @Column(name = "published_by", nullable = false)
  private UUID publishedBy;

  public static TemplateVersionEntity publish(
      UUID id,
      UUID templateId,
      int versionNumber,
      JsonNode content,
      JsonNode variablesSchema,
      String changeNote,
      UUID publishedBy) {
    TemplateVersionEntity v = new TemplateVersionEntity();
    v.id = id;
    v.templateId = templateId;
    v.versionNumber = versionNumber;
    v.content = content;
    v.variablesSchema = variablesSchema;
    v.changeNote = changeNote;
    v.publishedAt = Instant.now();
    v.publishedBy = publishedBy;
    return v;
  }
}
