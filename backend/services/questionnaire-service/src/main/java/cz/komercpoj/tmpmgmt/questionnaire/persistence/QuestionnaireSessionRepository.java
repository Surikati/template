package cz.komercpoj.tmpmgmt.questionnaire.persistence;

import cz.komercpoj.tmpmgmt.questionnaire.domain.SessionState;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionnaireSessionRepository
        extends JpaRepository<QuestionnaireSessionEntity, UUID> {

    List<QuestionnaireSessionEntity> findByStartedByAndStateOrderByStartedAtDesc(
            UUID userId, SessionState state);
}
