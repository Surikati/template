package cz.komercpoj.tmpmgmt.outbox;

import java.time.Instant;
import java.util.UUID;

/**
 * A domain event staged in the outbox table within the same transaction as the business change.
 * A publisher drains the table and sends to RabbitMQ.
 */
public record OutboxEvent(
        UUID eventId,
        String aggregateType,
        String aggregateId,
        String eventType,
        String payload,
        Instant occurredAt,
        Instant publishedAt) {

    public static OutboxEvent pending(
            String aggregateType, String aggregateId, String eventType, String payload) {
        return new OutboxEvent(
                UUID.randomUUID(),
                aggregateType,
                aggregateId,
                eventType,
                payload,
                Instant.now(),
                null);
    }
}
