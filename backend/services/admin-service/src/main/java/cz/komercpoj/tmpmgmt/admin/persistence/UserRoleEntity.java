package cz.komercpoj.tmpmgmt.admin.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_role")
@Getter
@Setter
@NoArgsConstructor
public class UserRoleEntity {

  @EmbeddedId private UserRoleKey id;

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
