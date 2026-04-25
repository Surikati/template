package cz.komercpoj.tmpmgmt.audit.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Composite primary key matching the partitioned {@code audit_event} table. Partition routing uses
 * {@code occurred_at}, so both fields must be part of the key.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class AuditEventKey implements Serializable {

  @Column(name = "event_id", nullable = false)
  private UUID eventId;

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

  public AuditEventKey(UUID eventId, Instant occurredAt) {
    this.eventId = eventId;
    this.occurredAt = occurredAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof AuditEventKey other)) return false;
    return Objects.equals(eventId, other.eventId) && Objects.equals(occurredAt, other.occurredAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(eventId, occurredAt);
  }
}
