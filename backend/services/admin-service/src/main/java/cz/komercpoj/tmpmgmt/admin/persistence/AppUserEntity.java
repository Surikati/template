package cz.komercpoj.tmpmgmt.admin.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "app_user")
@Getter
@Setter
@NoArgsConstructor
public class AppUserEntity implements Persistable<UUID> {

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

  @Column(name = "keycloak_subject", nullable = false, unique = true, length = 100)
  private String keycloakSubject;

  @Column(nullable = false, length = 200)
  private String username;

  @Column(nullable = false, length = 320)
  private String email;

  @Column(name = "display_name", length = 500)
  private String displayName;

  @Column(nullable = false)
  private boolean active;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "last_synced_at", nullable = false)
  private Instant lastSyncedAt;

  public static AppUserEntity createNew(
      String keycloakSubject, String username, String email, String displayName) {
    AppUserEntity u = new AppUserEntity();
    Instant now = Instant.now();
    u.id = UUID.randomUUID();
    u.keycloakSubject = keycloakSubject;
    u.username = username;
    u.email = email;
    u.displayName = displayName;
    u.active = true;
    u.createdAt = now;
    u.lastSyncedAt = now;
    return u;
  }

  public void refreshFromKeycloak(
      String username, String email, String displayName, boolean active) {
    this.username = username;
    this.email = email;
    this.displayName = displayName;
    this.active = active;
    this.lastSyncedAt = Instant.now();
  }
}
