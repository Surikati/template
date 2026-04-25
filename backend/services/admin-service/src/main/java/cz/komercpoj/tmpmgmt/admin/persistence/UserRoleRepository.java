package cz.komercpoj.tmpmgmt.admin.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRoleRepository extends JpaRepository<UserRoleEntity, UserRoleKey> {

  List<UserRoleEntity> findByIdUserId(UUID userId);

  void deleteByIdUserIdAndIdRoleCode(UUID userId, String roleCode);
}
