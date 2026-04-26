package cz.komercpoj.tmpmgmt.admin.api;

import cz.komercpoj.tmpmgmt.admin.api.dto.AppSettingsResponse;
import cz.komercpoj.tmpmgmt.admin.api.dto.UpdateAppSettingsRequest;
import cz.komercpoj.tmpmgmt.admin.application.AppSettingsService;
import cz.komercpoj.tmpmgmt.admin.persistence.AppSettingsEntity;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AppSettingsController {

  private final AppSettingsService service;

  public AppSettingsController(AppSettingsService service) {
    this.service = service;
  }

  /** Readable by anyone authenticated — clients need to know the active defaults. */
  @GetMapping("/api/v1/admin/settings")
  public AppSettingsResponse get() {
    return toResponse(service.get());
  }

  @PutMapping("/api/v1/admin/settings")
  @PreAuthorize("hasRole('ADMIN')")
  public AppSettingsResponse update(
      @Valid @RequestBody UpdateAppSettingsRequest req, @AuthenticationPrincipal Jwt jwt) {
    UUID actor = UUID.fromString(jwt.getSubject());
    return toResponse(service.update(req.locale(), req.timezone(), req.currency(), actor));
  }

  private static AppSettingsResponse toResponse(AppSettingsEntity e) {
    return new AppSettingsResponse(
        e.getLocale(), e.getTimezone(), e.getCurrency(), e.getUpdatedAt(), e.getUpdatedBy());
  }
}
