package cz.komercpoj.tmpmgmt.audit.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.komercpoj.tmpmgmt.audit.persistence.AuditEventEntity;
import cz.komercpoj.tmpmgmt.audit.persistence.AuditEventRepository;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consumes every event published by every service (routing-key pattern {@code #}) and appends to
 * the partitioned {@code audit_event} table. Idempotent — duplicates are skipped on
 * {@code (event_id, occurred_at)} conflict.
 */
@Component
public class AuditEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(AuditEventConsumer.class);

    /**
     * Payload fields we probe (in order) to infer the acting user. Falls back to {@code null}
     * when no identifiable actor is present (system-emitted events).
     */
    private static final List<String> ACTOR_CANDIDATES = List.of(
            "actorUserId", "publishedBy", "ownerUserId", "editorUserId",
            "startedBy", "requestedBy", "createdBy");

    private final AuditEventRepository repo;
    private final ObjectMapper mapper;

    public AuditEventConsumer(AuditEventRepository repo, ObjectMapper mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }

    @RabbitListener(queues = "#{auditQueue.name}")
    @Transactional
    public void onMessage(Message message) {
        MessageProperties props = message.getMessageProperties();
        String eventIdStr = props.getMessageId();
        Object aggregateType = props.getHeaders().get("aggregate-type");
        Object aggregateId = props.getHeaders().get("aggregate-id");
        Object eventType = props.getHeaders().get("event-type");

        if (eventIdStr == null || aggregateType == null || aggregateId == null || eventType == null) {
            log.warn("Skipping message with missing required headers: {}", props);
            return;
        }

        UUID eventId;
        try {
            eventId = UUID.fromString(eventIdStr);
        } catch (IllegalArgumentException ex) {
            log.warn("Skipping message with non-UUID message-id: {}", eventIdStr);
            return;
        }

        JsonNode payload;
        try {
            payload = mapper.readTree(new String(message.getBody(), StandardCharsets.UTF_8));
        } catch (Exception ex) {
            log.warn("Skipping message — invalid JSON payload: {}", ex.getMessage());
            return;
        }

        Instant occurredAt = extractOccurredAt(payload);

        if (repo.existsByEventIdAndOccurredAt(eventId, occurredAt)) {
            log.debug("Skipping duplicate audit event {}", eventId);
            return;
        }

        String correlationId = props.getCorrelationId();

        AuditEventEntity entity = AuditEventEntity.create(
                eventId,
                occurredAt,
                extractActor(payload),
                aggregateType.toString(),
                aggregateId.toString(),
                eventType.toString(),
                correlationId,
                payload);
        repo.save(entity);
    }

    private Instant extractOccurredAt(JsonNode payload) {
        JsonNode ts = payload.path("occurredAt");
        if (ts.isTextual()) {
            try {
                return Instant.parse(ts.asText());
            } catch (Exception ignored) { /* fall through */ }
        }
        return Instant.now();
    }

    private UUID extractActor(JsonNode payload) {
        for (String field : ACTOR_CANDIDATES) {
            JsonNode node = payload.path(field);
            if (node.isTextual()) {
                try {
                    return UUID.fromString(node.asText());
                } catch (IllegalArgumentException ignored) { /* try next */ }
            }
        }
        return null;
    }
}
