package cz.komercpoj.tmpmgmt.questionnaire.persistence;

import tools.jackson.databind.JsonNode;
import cz.komercpoj.tmpmgmt.questionnaire.domain.SessionState;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

/**
 * Independent aggregate — tracks a single user's traversal through a questionnaire. Not joined to
 * {@link QuestionnaireEntity} at the JPA level; the link is a plain UUID reference.
 */
@Entity
@Table(name = "questionnaire_session")
@Getter
@Setter
@NoArgsConstructor
public class QuestionnaireSessionEntity implements Persistable<UUID> {

  @Id private UUID id;

  /**
   * Routes {@code save()} through {@code persist()} instead of {@code merge()} so the original
   * reference stays managed and dirty checking flushes subsequent mutations. See {@code
   * transactional_patterns.md}.
   */
  @Transient private boolean isNew = true;

  @Override
  public boolean isNew() {
    return isNew;
  }

  @PostPersist
  @PostLoad
  void markNotNew() {
    this.isNew = false;
  }

  @Column(name = "questionnaire_id", nullable = false)
  private UUID questionnaireId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private SessionState state;

  @Column(name = "started_by", nullable = false)
  private UUID startedBy;

  @Column(name = "started_at", nullable = false)
  private Instant startedAt;

  @Column(name = "completed_at")
  private Instant completedAt;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false, columnDefinition = "jsonb")
  private JsonNode answers;

  @Column(name = "current_section_id")
  private UUID currentSectionId;

  public static QuestionnaireSessionEntity start(
      UUID id, UUID questionnaireId, UUID startedBy, JsonNode emptyAnswers) {
    QuestionnaireSessionEntity s = new QuestionnaireSessionEntity();
    s.id = id;
    s.questionnaireId = questionnaireId;
    s.state = SessionState.IN_PROGRESS;
    s.startedBy = startedBy;
    s.startedAt = Instant.now();
    s.answers = emptyAnswers;
    return s;
  }

  public void markCompleted() {
    this.state = SessionState.COMPLETED;
    this.completedAt = Instant.now();
  }

  public void markAbandoned() {
    this.state = SessionState.ABANDONED;
    this.completedAt = Instant.now();
  }
}
