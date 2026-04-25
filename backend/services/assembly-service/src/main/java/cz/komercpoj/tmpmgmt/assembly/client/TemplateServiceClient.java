package cz.komercpoj.tmpmgmt.assembly.client;

import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "template-service", url = "${tmpmgmt.clients.template-service-url}")
public interface TemplateServiceClient {

  @GetMapping("/api/v1/templates/{templateId}/versions/{versionNumber}")
  TemplateVersionDto getVersion(@PathVariable UUID templateId, @PathVariable int versionNumber);
}
