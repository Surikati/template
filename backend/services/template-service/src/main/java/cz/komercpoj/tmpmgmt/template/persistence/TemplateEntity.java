package cz.komercpoj.tmpmgmt.template.persistence;

import cz.komercpoj.tmpmgmt.template.domain.TemplateStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "template")
@Getter
@Setter
@NoArgsConstructor
public class TemplateEntity {

  @Id private UUID id;

  @Column(nullable = false, unique = true, length = 200)
  private String slug;

  @Column(nullable = false, length = 500)
  private String name;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(length = 100)
  private String category;

  @JdbcTypeCode(SqlTypes.ARRAY)
  @Column(columnDefinition = "text[]", nullable = false)
  private String[] tags = new String[0];

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private TemplateStatus status;

  @Column(name = "owner_user_id", nullable = false)
  private UUID ownerUserId;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public static TemplateEntity newActive(
      UUID id, String slug, String name, String description, String category, UUID ownerUserId) {
    TemplateEntity e = new TemplateEntity();
    Instant now = Instant.now();
    e.id = id;
    e.slug = slug;
    e.name = name;
    e.description = description;
    e.category = category;
    e.status = TemplateStatus.ACTIVE;
    e.ownerUserId = ownerUserId;
    e.createdAt = now;
    e.updatedAt = now;
    return e;
  }

  public void touchUpdated() {
    this.updatedAt = Instant.now();
  }
}
