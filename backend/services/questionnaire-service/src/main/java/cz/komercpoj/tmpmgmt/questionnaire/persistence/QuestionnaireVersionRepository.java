package cz.komercpoj.tmpmgmt.questionnaire.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface QuestionnaireVersionRepository
        extends JpaRepository<QuestionnaireVersionEntity, UUID> {

    List<QuestionnaireVersionEntity> findByQuestionnaireIdOrderByVersionNumberDesc(UUID questionnaireId);

    Optional<QuestionnaireVersionEntity> findByQuestionnaireIdAndVersionNumber(
            UUID questionnaireId, int versionNumber);

    @Query("SELECT COALESCE(MAX(v.versionNumber), 0) FROM QuestionnaireVersionEntity v"
            + " WHERE v.questionnaireId = :questionnaireId")
    int findMaxVersionNumber(UUID questionnaireId);
}
