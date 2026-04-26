package cz.komercpoj.tmpmgmt.questionnaire.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuestionnaireVersionRepository
    extends JpaRepository<QuestionnaireVersionEntity, UUID> {

  List<QuestionnaireVersionEntity> findByQuestionnaireIdOrderByVersionNumberDesc(
      UUID questionnaireId);

  Optional<QuestionnaireVersionEntity> findByQuestionnaireIdAndVersionNumber(
      UUID questionnaireId, int versionNumber);

  @Query(
      "SELECT COALESCE(MAX(v.versionNumber), 0) FROM QuestionnaireVersionEntity v"
          + " WHERE v.questionnaireId = :questionnaireId")
  int findMaxVersionNumber(UUID questionnaireId);

  /**
   * Deletes versions older than {@code cutoff} except the latest one per questionnaire. The latest
   * is identified as the row with the highest {@code version_number} for each {@code
   * questionnaire_id}, regardless of its {@code published_at} — so a questionnaire that hasn't been
   * republished in years still keeps its single most recent snapshot. Returns the deleted row
   * count.
   */
  @Modifying
  @Query(
      "DELETE FROM QuestionnaireVersionEntity v"
          + " WHERE v.publishedAt < :cutoff"
          + " AND v.versionNumber < ("
          + "   SELECT MAX(q.versionNumber) FROM QuestionnaireVersionEntity q"
          + "   WHERE q.questionnaireId = v.questionnaireId"
          + " )")
  int deleteOlderThanKeepingLatest(@Param("cutoff") Instant cutoff);
}
