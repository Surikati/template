package cz.komercpoj.tmpmgmt.audit.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

public record AuditEventResponse(
    UUID eventId,
    Instant occurredAt,
    UUID actorUserId,
    String aggregateType,
    String aggregateId,
    String eventType,
    String correlationId,
    JsonNode payload) {}
