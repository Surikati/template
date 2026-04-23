package cz.komercpoj.tmpmgmt.admin.application;

import cz.komercpoj.tmpmgmt.admin.persistence.*;
import cz.komercpoj.tmpmgmt.common.NotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoleAdminService {

    private final AppUserRepository users;
    private final RoleDefinitionRepository roles;
    private final UserRoleRepository assignments;

    public RoleAdminService(
            AppUserRepository users,
            RoleDefinitionRepository roles,
            UserRoleRepository assignments) {
        this.users = users;
        this.roles = roles;
        this.assignments = assignments;
    }

    @Transactional(readOnly = true)
    public List<RoleDefinitionEntity> listRoleDefinitions() {
        return roles.findAll();
    }

    @Transactional(readOnly = true)
    public List<UserRoleEntity> listUserRoles(UUID userId) {
        ensureUserExists(userId);
        return assignments.findByIdUserId(userId);
    }

    @Transactional
    public UserRoleEntity grant(UUID userId, String roleCode, UUID grantedBy) {
        ensureUserExists(userId);
        if (!roles.existsById(roleCode)) {
            throw new NotFoundException(
                    "role.not_found", "Role code unknown: " + roleCode);
        }
        UserRoleKey key = new UserRoleKey(userId, roleCode);
        return assignments.findById(key)
                .orElseGet(() -> assignments.save(UserRoleEntity.grant(userId, roleCode, grantedBy)));
    }

    @Transactional
    public void revoke(UUID userId, String roleCode) {
        assignments.deleteByIdUserIdAndIdRoleCode(userId, roleCode);
    }

    private void ensureUserExists(UUID userId) {
        if (!users.existsById(userId)) {
            throw new NotFoundException("user.not_found", "User not found: " + userId);
        }
    }
}
