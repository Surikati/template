package cz.komercpoj.tmpmgmt.admin.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUserEntity, UUID> {

  Optional<AppUserEntity> findByKeycloakSubject(String keycloakSubject);

  List<AppUserEntity> findByActiveTrue();
}
