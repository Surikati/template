package cz.komercpoj.tmpmgmt.questionnaire.api;

import cz.komercpoj.tmpmgmt.questionnaire.api.dto.QuestionResponse;
import cz.komercpoj.tmpmgmt.questionnaire.api.dto.QuestionnaireResponse;
import cz.komercpoj.tmpmgmt.questionnaire.api.dto.SectionResponse;
import cz.komercpoj.tmpmgmt.questionnaire.api.dto.SessionResponse;
import cz.komercpoj.tmpmgmt.questionnaire.persistence.QuestionnaireEntity;
import cz.komercpoj.tmpmgmt.questionnaire.persistence.QuestionnaireQuestionEntity;
import cz.komercpoj.tmpmgmt.questionnaire.persistence.QuestionnaireSectionEntity;
import cz.komercpoj.tmpmgmt.questionnaire.persistence.QuestionnaireSessionEntity;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface QuestionnaireMapper {

    QuestionnaireResponse toResponse(QuestionnaireEntity entity);

    SectionResponse toResponse(QuestionnaireSectionEntity entity);

    QuestionResponse toResponse(QuestionnaireQuestionEntity entity);

    List<SectionResponse> toSectionResponses(List<QuestionnaireSectionEntity> entities);

    SessionResponse toResponse(QuestionnaireSessionEntity entity);
}
