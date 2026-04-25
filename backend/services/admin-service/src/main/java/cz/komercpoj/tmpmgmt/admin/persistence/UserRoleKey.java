package cz.komercpoj.tmpmgmt.admin.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class UserRoleKey implements Serializable {

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "role_code", nullable = false, length = 50)
  private String roleCode;

  public UserRoleKey(UUID userId, String roleCode) {
    this.userId = userId;
    this.roleCode = roleCode;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof UserRoleKey other)) return false;
    return Objects.equals(userId, other.userId) && Objects.equals(roleCode, other.roleCode);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, roleCode);
  }
}
