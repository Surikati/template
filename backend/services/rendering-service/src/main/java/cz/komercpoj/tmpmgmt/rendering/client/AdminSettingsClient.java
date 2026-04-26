package cz.komercpoj.tmpmgmt.rendering.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Reads the singleton {@code app_settings} row from admin-service. Used by {@link
 * cz.komercpoj.tmpmgmt.rendering.application.AppSettingsCache} as the fallback locale source when a
 * request doesn't supply X-Locale / X-Timezone / X-Currency headers.
 */
@FeignClient(name = "admin-service", url = "${tmpmgmt.clients.admin-service-url}")
public interface AdminSettingsClient {

  @GetMapping("/api/v1/admin/settings")
  AppSettingsDto fetch();

  record AppSettingsDto(String locale, String timezone, String currency) {}
}
