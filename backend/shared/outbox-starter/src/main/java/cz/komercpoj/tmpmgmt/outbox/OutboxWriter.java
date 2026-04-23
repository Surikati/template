package cz.komercpoj.tmpmgmt.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.komercpoj.tmpmgmt.common.DomainException;
import java.sql.Timestamp;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Stages events in the {@code outbox_event} table. Must be called from within an existing business
 * transaction so the event is persisted atomically with the state change.
 */
@Component
public class OutboxWriter {

    private static final String INSERT_SQL =
            "INSERT INTO outbox_event "
                    + "(event_id, aggregate_type, aggregate_id, event_type, payload, occurred_at) "
                    + "VALUES (?, ?, ?, ?, ?::jsonb, ?)";

    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public OutboxWriter(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void stage(String aggregateType, String aggregateId, String eventType, Object payload) {
        String json;
        try {
            json = mapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new DomainException("outbox.serialization_failed", e.getMessage(), e);
        }
        OutboxEvent evt = OutboxEvent.pending(aggregateType, aggregateId, eventType, json);
        jdbc.update(
                INSERT_SQL,
                evt.eventId(),
                evt.aggregateType(),
                evt.aggregateId(),
                evt.eventType(),
                evt.payload(),
                Timestamp.from(evt.occurredAt()));
    }
}
