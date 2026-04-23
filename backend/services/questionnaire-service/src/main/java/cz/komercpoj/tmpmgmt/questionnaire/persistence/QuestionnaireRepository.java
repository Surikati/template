package cz.komercpoj.tmpmgmt.questionnaire.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionnaireRepository extends JpaRepository<QuestionnaireEntity, UUID> {

    Optional<QuestionnaireEntity> findByTemplateIdAndTemplateVersionNumber(
            UUID templateId, int templateVersionNumber);

    boolean existsByTemplateIdAndTemplateVersionNumber(UUID templateId, int templateVersionNumber);
}
