package cz.komercpoj.tmpmgmt.audit.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AuditEventRepository extends JpaRepository<AuditEventEntity, AuditEventKey> {

    @Query("SELECT e FROM AuditEventEntity e "
            + "WHERE e.aggregateType = :aggregateType AND e.aggregateId = :aggregateId "
            + "ORDER BY e.id.occurredAt DESC")
    List<AuditEventEntity> findByAggregate(String aggregateType, String aggregateId, Pageable page);

    @Query("SELECT e FROM AuditEventEntity e "
            + "WHERE e.actorUserId = :actorUserId "
            + "ORDER BY e.id.occurredAt DESC")
    List<AuditEventEntity> findByActor(UUID actorUserId, Pageable page);

    @Query("SELECT e FROM AuditEventEntity e ORDER BY e.id.occurredAt DESC")
    List<AuditEventEntity> findRecent(Pageable page);

    /** Idempotency guard — true if the event is already stored. */
    @Query("SELECT COUNT(e) > 0 FROM AuditEventEntity e "
            + "WHERE e.id.eventId = :eventId AND e.id.occurredAt = :occurredAt")
    boolean existsByEventIdAndOccurredAt(UUID eventId, Instant occurredAt);
}
