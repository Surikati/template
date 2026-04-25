package cz.komercpoj.tmpmgmt.clause.persistence;

import cz.komercpoj.tmpmgmt.clause.domain.ClauseStatus;
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
@Table(name = "clause")
@Getter
@Setter
@NoArgsConstructor
public class ClauseEntity implements Persistable<UUID> {

  @Id private UUID id;

  /**
   * Routes {@code save()} through {@code persist()} instead of {@code merge()} so the original
   * reference stays managed and dirty checking flushes subsequent mutations. See {@code
   * transactional_patterns.md}.
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
  private ClauseStatus status;

  @Column(name = "owner_user_id", nullable = false)
  private UUID ownerUserId;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public static ClauseEntity newActive(
      UUID id, String slug, String name, String description, String category, UUID ownerUserId) {
    ClauseEntity c = new ClauseEntity();
    Instant now = Instant.now();
    c.id = id;
    c.slug = slug;
    c.name = name;
    c.description = description;
    c.category = category;
    c.status = ClauseStatus.ACTIVE;
    c.ownerUserId = ownerUserId;
    c.createdAt = now;
    c.updatedAt = now;
    return c;
  }

  public void touchUpdated() {
    this.updatedAt = Instant.now();
  }
}
