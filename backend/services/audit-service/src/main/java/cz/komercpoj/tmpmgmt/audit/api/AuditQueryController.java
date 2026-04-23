package cz.komercpoj.tmpmgmt.audit.api;

import cz.komercpoj.tmpmgmt.audit.api.dto.AuditEventResponse;
import cz.komercpoj.tmpmgmt.audit.persistence.AuditEventEntity;
import cz.komercpoj.tmpmgmt.audit.persistence.AuditEventRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/audit/events")
@PreAuthorize("hasRole('ADMIN')")
public class AuditQueryController {

    private static final int MAX_LIMIT = 500;

    private final AuditEventRepository repo;

    public AuditQueryController(AuditEventRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<AuditEventResponse> list(
            @RequestParam(required = false) String aggregateType,
            @RequestParam(required = false) String aggregateId,
            @RequestParam(required = false) UUID actorUserId,
            @RequestParam(required = false, defaultValue = "100") int limit) {

        int capped = Math.min(Math.max(1, limit), MAX_LIMIT);
        var page = PageRequest.of(0, capped);

        List<AuditEventEntity> results;
        if (aggregateType != null && aggregateId != null) {
            results = repo.findByAggregate(aggregateType, aggregateId, page);
        } else if (actorUserId != null) {
            results = repo.findByActor(actorUserId, page);
        } else {
            results = repo.findRecent(page);
        }

        return results.stream().map(AuditQueryController::toResponse).toList();
    }

    private static AuditEventResponse toResponse(AuditEventEntity e) {
        return new AuditEventResponse(
                e.getId().getEventId(),
                e.getId().getOccurredAt(),
                e.getActorUserId(),
                e.getAggregateType(),
                e.getAggregateId(),
                e.getEventType(),
                e.getCorrelationId(),
                e.getPayload());
    }
}
