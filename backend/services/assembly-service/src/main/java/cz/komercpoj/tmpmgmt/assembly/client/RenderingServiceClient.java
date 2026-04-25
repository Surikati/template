package cz.komercpoj.tmpmgmt.assembly.client;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "rendering-service", url = "${tmpmgmt.clients.rendering-service-url}")
public interface RenderingServiceClient {

  @PostMapping("/api/v1/render")
  RenderResponse render(@RequestBody RenderRequest request);

  enum RenderFormat {
    DOCX,
    PDF,
    HTML
  }

  record RenderRequest(JsonNode content, Map<String, Object> data, RenderFormat format) {}

  record RenderResponse(RenderFormat format, String filename, byte[] content) {}
}
