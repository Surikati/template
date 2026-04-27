package cz.komercpoj.tmpmgmt.questionnaire.application;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import cz.komercpoj.tmpmgmt.common.ConflictException;
import cz.komercpoj.tmpmgmt.common.NotFoundException;
import cz.komercpoj.tmpmgmt.outbox.OutboxWriter;
import cz.komercpoj.tmpmgmt.questionnaire.application.QuestionnaireCommands.CreateQuestionnaire;
import cz.komercpoj.tmpmgmt.questionnaire.application.QuestionnaireCommands.PublishQuestionnaireVersion;
import cz.komercpoj.tmpmgmt.questionnaire.application.QuestionnaireCommands.QuestionInput;
import cz.komercpoj.tmpmgmt.questionnaire.application.QuestionnaireCommands.ReplaceStructure;
import cz.komercpoj.tmpmgmt.questionnaire.application.QuestionnaireCommands.SectionInput;
import cz.komercpoj.tmpmgmt.questionnaire.application.events.QuestionnaireEvents;
import cz.komercpoj.tmpmgmt.questionnaire.persistence.QuestionnaireEntity;
import cz.komercpoj.tmpmgmt.questionnaire.persistence.QuestionnaireQuestionEntity;
import cz.komercpoj.tmpmgmt.questionnaire.persistence.QuestionnaireRepository;
import cz.komercpoj.tmpmgmt.questionnaire.persistence.QuestionnaireSectionEntity;
import cz.komercpoj.tmpmgmt.questionnaire.persistence.QuestionnaireVersionEntity;
import cz.komercpoj.tmpmgmt.questionnaire.persistence.QuestionnaireVersionRepository;
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
  private final QuestionnaireVersionRepository versionRepo;
  private final OutboxWriter outbox;
  private final ObjectMapper mapper;

  public QuestionnaireService(
      QuestionnaireRepository repo,
      QuestionnaireVersionRepository versionRepo,
      OutboxWriter outbox,
      ObjectMapper mapper) {
    this.repo = repo;
    this.versionRepo = versionRepo;
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
              + cmd.templateId()
              + " v"
              + cmd.templateVersionNumber());
    }
    UUID id = UUID.randomUUID();
    QuestionnaireEntity q =
        QuestionnaireEntity.create(id, cmd.templateId(), cmd.templateVersionNumber(), cmd.name());
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

  /**
   * Snapshots the current draft structure into an immutable {@link QuestionnaireVersionEntity}.
   * Subsequent {@link #replaceStructure} edits do not affect already-published versions — sessions
   * started against a specific version see the structure as it was at publish time.
   */
  @Transactional
  public QuestionnaireVersionEntity publishVersion(PublishQuestionnaireVersion cmd) {
    QuestionnaireEntity q = getById(cmd.questionnaireId());
    int nextVersion = versionRepo.findMaxVersionNumber(q.getId()) + 1;
    UUID versionId = UUID.randomUUID();
    QuestionnaireVersionEntity v =
        QuestionnaireVersionEntity.publish(
            versionId,
            q.getId(),
            nextVersion,
            q.getName(),
            snapshotStructure(q),
            cmd.publishedBy());
    versionRepo.save(v);

    outbox.stage(
        QuestionnaireEvents.AGGREGATE_TYPE,
        q.getId().toString(),
        QuestionnaireEvents.TYPE_VERSION_PUBLISHED,
        new QuestionnaireEvents.QuestionnaireVersionPublished(
            q.getId(), versionId, nextVersion, cmd.publishedBy(), Instant.now()));
    return v;
  }

  @Transactional(readOnly = true)
  public List<QuestionnaireVersionEntity> listVersions(UUID questionnaireId) {
    getById(questionnaireId); // 404 vs empty list
    return versionRepo.findByQuestionnaireIdOrderByVersionNumberDesc(questionnaireId);
  }

  @Transactional(readOnly = true)
  public QuestionnaireVersionEntity getVersion(UUID questionnaireId, int versionNumber) {
    return versionRepo
        .findByQuestionnaireIdAndVersionNumber(questionnaireId, versionNumber)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "questionnaire.version_not_found",
                    "No version " + versionNumber + " for questionnaire " + questionnaireId));
  }

  private List<QuestionnaireSectionEntity> toSectionEntities(List<SectionInput> inputs) {
    List<QuestionnaireSectionEntity> out = new ArrayList<>();
    for (SectionInput si : inputs) {
      QuestionnaireSectionEntity s =
          QuestionnaireSectionEntity.create(
              UUID.randomUUID(), si.ordinal(), si.title(), si.visibilityRule());
      for (QuestionInput qi : si.questions()) {
        s.addQuestion(
            QuestionnaireQuestionEntity.create(
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

  /**
   * Builds a JSON array shaped like {@code QuestionnaireResponse.sections} — embedded so the
   * snapshot round-trips through the API mapper without any extra transformation step.
   */
  private JsonNode snapshotStructure(QuestionnaireEntity q) {
    ArrayNode sections = mapper.createArrayNode();
    for (QuestionnaireSectionEntity sec : q.getSections()) {
      ObjectNode s = mapper.createObjectNode();
      s.put("id", sec.getId().toString());
      s.put("ordinal", sec.getOrdinal());
      s.put("title", sec.getTitle());
      s.put("visibilityRule", sec.getVisibilityRule());
      ArrayNode questions = mapper.createArrayNode();
      for (QuestionnaireQuestionEntity qq : sec.getQuestions()) {
        ObjectNode qn = mapper.createObjectNode();
        qn.put("id", qq.getId().toString());
        qn.put("ordinal", qq.getOrdinal());
        qn.put("variablePath", qq.getVariablePath());
        qn.put("label", qq.getLabel());
        qn.put("questionType", qq.getQuestionType().name());
        qn.set("validation", qq.getValidation());
        qn.put("visibilityRule", qq.getVisibilityRule());
        qn.set("options", qq.getOptions());
        questions.add(qn);
      }
      s.set("questions", questions);
      sections.add(s);
    }
    return sections;
  }

  private NotFoundException notFound(UUID id) {
    return new NotFoundException("questionnaire.not_found", "Questionnaire not found: " + id);
  }
}
