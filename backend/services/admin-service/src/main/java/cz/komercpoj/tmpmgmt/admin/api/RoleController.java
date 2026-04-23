package cz.komercpoj.tmpmgmt.admin.api;

import cz.komercpoj.tmpmgmt.admin.api.dto.RoleResponse;
import cz.komercpoj.tmpmgmt.admin.api.dto.UserRoleResponse;
import cz.komercpoj.tmpmgmt.admin.application.RoleAdminService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@PreAuthorize("hasRole('ADMIN')")
public class RoleController {

    private final RoleAdminService service;

    public RoleController(RoleAdminService service) {
        this.service = service;
    }

    @GetMapping("/api/v1/roles")
    public List<RoleResponse> listDefinitions() {
        return service.listRoleDefinitions().stream()
                .map(r -> new RoleResponse(r.getCode(), r.getDisplayName(), r.getDescription()))
                .toList();
    }

    @GetMapping("/api/v1/users/{userId}/roles")
    public List<UserRoleResponse> listUserRoles(@PathVariable UUID userId) {
        return service.listUserRoles(userId).stream()
                .map(ur -> new UserRoleResponse(
                        ur.getId().getUserId(),
                        ur.getId().getRoleCode(),
                        ur.getGrantedAt(),
                        ur.getGrantedBy()))
                .toList();
    }

    @PostMapping("/api/v1/users/{userId}/roles/{roleCode}")
    public UserRoleResponse grant(
            @PathVariable UUID userId,
            @PathVariable String roleCode,
            @AuthenticationPrincipal Jwt jwt) {
        var ur = service.grant(userId, roleCode, UUID.fromString(jwt.getSubject()));
        return new UserRoleResponse(
                ur.getId().getUserId(), ur.getId().getRoleCode(),
                ur.getGrantedAt(), ur.getGrantedBy());
    }

    @DeleteMapping("/api/v1/users/{userId}/roles/{roleCode}")
    public ResponseEntity<Void> revoke(@PathVariable UUID userId, @PathVariable String roleCode) {
        service.revoke(userId, roleCode);
        return ResponseEntity.noContent().build();
    }
}
