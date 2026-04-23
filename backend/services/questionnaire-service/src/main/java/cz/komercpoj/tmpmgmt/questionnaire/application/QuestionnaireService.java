package cz.komercpoj.tmpmgmt.questionnaire.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.komercpoj.tmpmgmt.common.ConflictException;
import cz.komercpoj.tmpmgmt.common.NotFoundException;
import cz.komercpoj.tmpmgmt.outbox.OutboxWriter;
import cz.komercpoj.tmpmgmt.questionnaire.application.QuestionnaireCommands.CreateQuestionnaire;
import cz.komercpoj.tmpmgmt.questionnaire.application.QuestionnaireCommands.QuestionInput;
import cz.komercpoj.tmpmgmt.questionnaire.application.QuestionnaireCommands.ReplaceStructure;
import cz.komercpoj.tmpmgmt.questionnaire.application.QuestionnaireCommands.SectionInput;
import cz.komercpoj.tmpmgmt.questionnaire.application.events.QuestionnaireEvents;
import cz.komercpoj.tmpmgmt.questionnaire.persistence.QuestionnaireEntity;
import cz.komercpoj.tmpmgmt.questionnaire.persistence.QuestionnaireQuestionEntity;
import cz.komercpoj.tmpmgmt.questionnaire.persistence.QuestionnaireRepository;
import cz.komercpoj.tmpmgmt.questionnaire.persistence.QuestionnaireSectionEntity;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuestionnaireService {

    private final QuestionnaireRepository repo;
    private final OutboxWriter outbox;
    private final ObjectMapper mapper;

    public QuestionnaireService(
            QuestionnaireRepository repo, OutboxWriter outbox, ObjectMapper mapper) {
        this.repo = repo;
        this.outbox = outbox;
        this.mapper = mapper;
    }

    @Transactional
    public QuestionnaireEntity create(CreateQuestionnaire cmd) {
        if (repo.existsByTemplateIdAndTemplateVersionNumber(
                cmd.templateId(), cmd.templateVersionNumber())) {
            throw new ConflictException(
                    "questionnaire.already_exists",
                    "Questionnaire already exists for template "
                            + cmd.templateId() + " v" + cmd.templateVersionNumber());
        }
        UUID id = UUID.randomUUID();
        QuestionnaireEntity q = QuestionnaireEntity.create(
                id, cmd.templateId(), cmd.templateVersionNumber(), cmd.name());
        q.replaceSections(toSectionEntities(cmd.sections()));
        repo.save(q);

        outbox.stage(
                QuestionnaireEvents.AGGREGATE_TYPE,
                id.toString(),
                QuestionnaireEvents.TYPE_CREATED,
                new QuestionnaireEvents.QuestionnaireCreated(
                        id,
                        cmd.templateId(),
                        cmd.templateVersionNumber(),
                        cmd.name(),
                        q.getSections().size(),
                        Instant.now()));
        return q;
    }

    @Transactional(readOnly = true)
    public QuestionnaireEntity getById(UUID id) {
        return repo.findById(id).orElseThrow(() -> notFound(id));
    }

    @Transactional(readOnly = true)
    public Optional<QuestionnaireEntity> findByTemplateVersion(UUID templateId, int versionNumber) {
        return repo.findByTemplateIdAndTemplateVersionNumber(templateId, versionNumber);
    }

    @Transactional
    public QuestionnaireEntity replaceStructure(ReplaceStructure cmd) {
        QuestionnaireEntity q = getById(cmd.questionnaireId());
        q.setName(cmd.name());
        q.replaceSections(toSectionEntities(cmd.sections()));
        q.touchUpdated();

        outbox.stage(
                QuestionnaireEvents.AGGREGATE_TYPE,
                q.getId().toString(),
                QuestionnaireEvents.TYPE_UPDATED,
                new QuestionnaireEvents.QuestionnaireUpdated(
                        q.getId(), q.getSections().size(), Instant.now()));
        return q;
    }

    private List<QuestionnaireSectionEntity> toSectionEntities(List<SectionInput> inputs) {
        List<QuestionnaireSectionEntity> out = new ArrayList<>();
        for (SectionInput si : inputs) {
            QuestionnaireSectionEntity s = QuestionnaireSectionEntity.create(
                    UUID.randomUUID(), si.ordinal(), si.title(), si.visibilityRule());
            for (QuestionInput qi : si.questions()) {
                s.addQuestion(QuestionnaireQuestionEntity.create(
                        UUID.randomUUID(),
                        qi.ordinal(),
                        qi.variablePath(),
                        qi.label(),
                        qi.questionType(),
                        qi.validation(),
                        qi.visibilityRule(),
                        qi.options()));
            }
            out.add(s);
        }
        return out;
    }

    private NotFoundException notFound(UUID id) {
        return new NotFoundException("questionnaire.not_found", "Questionnaire not found: " + id);
    }
}
