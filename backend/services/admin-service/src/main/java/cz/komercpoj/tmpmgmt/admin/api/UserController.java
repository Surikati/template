package cz.komercpoj.tmpmgmt.admin.api;

import cz.komercpoj.tmpmgmt.admin.api.dto.SyncResponse;
import cz.komercpoj.tmpmgmt.admin.api.dto.UserResponse;
import cz.komercpoj.tmpmgmt.admin.application.AdminUserService;
import cz.komercpoj.tmpmgmt.admin.persistence.AppUserEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final AdminUserService service;

    public UserController(AdminUserService service) {
        this.service = service;
    }

    @GetMapping
    public List<UserResponse> list() {
        return service.listActive().stream().map(UserController::toResponse).toList();
    }

    @GetMapping("/{id}")
    public UserResponse get(@PathVariable UUID id) {
        return toResponse(service.getById(id));
    }

    @PostMapping("/sync")
    public SyncResponse sync() {
        var r = service.sync();
        return new SyncResponse(r.created(), r.updated(), r.totalFetched());
    }

    private static UserResponse toResponse(AppUserEntity u) {
        return new UserResponse(
                u.getId(),
                u.getKeycloakSubject(),
                u.getUsername(),
                u.getEmail(),
                u.getDisplayName(),
                u.isActive(),
                u.getCreatedAt(),
                u.getLastSyncedAt());
    }
}
