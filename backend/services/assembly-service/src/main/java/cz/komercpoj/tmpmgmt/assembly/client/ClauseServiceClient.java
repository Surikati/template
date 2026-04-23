package cz.komercpoj.tmpmgmt.assembly.client;

import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "clause-service", url = "${tmpmgmt.clients.clause-service-url}")
public interface ClauseServiceClient {

    @GetMapping("/api/v1/clauses/{clauseId}/versions/{versionNumber}")
    ClauseVersionDto getVersion(@PathVariable UUID clauseId, @PathVariable int versionNumber);
}
