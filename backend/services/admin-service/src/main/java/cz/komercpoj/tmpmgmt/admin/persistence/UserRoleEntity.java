package cz.komercpoj.tmpmgmt.admin.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "user_role")
@Getter
@Setter
@NoArgsConstructor
public class UserRoleEntity implements Persistable<UserRoleKey> {

  @EmbeddedId private UserRoleKey id;

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

  @Column(name = "granted_at", nullable = false)
  private Instant grantedAt;

  @Column(name = "granted_by")
  private UUID grantedBy;

  public static UserRoleEntity grant(UUID userId, String roleCode, UUID grantedBy) {
    UserRoleEntity e = new UserRoleEntity();
    e.id = new UserRoleKey(userId, roleCode);
    e.grantedAt = Instant.now();
    e.grantedBy = grantedBy;
    return e;
  }
}
