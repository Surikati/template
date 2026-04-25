package cz.komercpoj.tmpmgmt.admin.application;

import cz.komercpoj.tmpmgmt.admin.config.KeycloakProperties;
import cz.komercpoj.tmpmgmt.admin.persistence.AppUserEntity;
import cz.komercpoj.tmpmgmt.admin.persistence.AppUserRepository;
import cz.komercpoj.tmpmgmt.common.NotFoundException;
import java.util.List;
import java.util.UUID;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserService {

  private static final Logger log = LoggerFactory.getLogger(AdminUserService.class);

  private final AppUserRepository users;
  private final Keycloak keycloak;
  private final KeycloakProperties props;

  public AdminUserService(AppUserRepository users, Keycloak keycloak, KeycloakProperties props) {
    this.users = users;
    this.keycloak = keycloak;
    this.props = props;
  }

  /** Fetches all users from Keycloak and upserts into {@code app_user}. */
  @Transactional
  public SyncResult sync() {
    List<UserRepresentation> kcUsers = keycloak.realm(props.realm()).users().list(0, 10_000);
    int created = 0, updated = 0;
    for (UserRepresentation kc : kcUsers) {
      var existing = users.findByKeycloakSubject(kc.getId());
      String displayName = buildDisplayName(kc);
      if (existing.isEmpty()) {
        users.save(
            AppUserEntity.createNew(kc.getId(), kc.getUsername(), kc.getEmail(), displayName));
        created++;
      } else {
        AppUserEntity e = existing.get();
        e.refreshFromKeycloak(
            kc.getUsername(), kc.getEmail(), displayName, Boolean.TRUE.equals(kc.isEnabled()));
        updated++;
      }
    }
    log.info(
        "Keycloak sync: {} created, {} updated (total fetched: {})",
        created,
        updated,
        kcUsers.size());
    return new SyncResult(created, updated, kcUsers.size());
  }

  @Transactional(readOnly = true)
  public List<AppUserEntity> listActive() {
    return users.findByActiveTrue();
  }

  @Transactional(readOnly = true)
  public AppUserEntity getById(UUID id) {
    return users
        .findById(id)
        .orElseThrow(() -> new NotFoundException("user.not_found", "User not found: " + id));
  }

  private static String buildDisplayName(UserRepresentation kc) {
    String first = kc.getFirstName() == null ? "" : kc.getFirstName();
    String last = kc.getLastName() == null ? "" : kc.getLastName();
    String composed = (first + " " + last).trim();
    return composed.isEmpty() ? kc.getUsername() : composed;
  }

  public record SyncResult(int created, int updated, int totalFetched) {}
}
