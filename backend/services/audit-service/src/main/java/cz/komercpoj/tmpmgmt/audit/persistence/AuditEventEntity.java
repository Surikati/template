package cz.komercpoj.tmpmgmt.audit.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "audit_event")
@Getter
@Setter
@NoArgsConstructor
public class AuditEventEntity {

  @EmbeddedId private AuditEventKey id;

  @Column(name = "actor_user_id")
  private UUID actorUserId;

  @Column(name = "aggregate_type", nullable = false, length = 100)
  private String aggregateType;

  @Column(name = "aggregate_id", nullable = false, length = 100)
  private String aggregateId;

  @Column(name = "event_type", nullable = false, length = 100)
  private String eventType;

  @Column(name = "correlation_id", length = 100)
  private String correlationId;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false, columnDefinition = "jsonb")
  private JsonNode payload;

  public static AuditEventEntity create(
      UUID eventId,
      Instant occurredAt,
      UUID actorUserId,
      String aggregateType,
      String aggregateId,
      String eventType,
      String correlationId,
      JsonNode payload) {
    AuditEventEntity e = new AuditEventEntity();
    e.id = new AuditEventKey(eventId, occurredAt);
    e.actorUserId = actorUserId;
    e.aggregateType = aggregateType;
    e.aggregateId = aggregateId;
    e.eventType = eventType;
    e.correlationId = correlationId;
    e.payload = payload;
    return e;
  }
}
