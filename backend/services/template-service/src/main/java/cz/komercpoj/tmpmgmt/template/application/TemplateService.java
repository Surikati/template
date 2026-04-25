package cz.komercpoj.tmpmgmt.template.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.komercpoj.tmpmgmt.common.ConflictException;
import cz.komercpoj.tmpmgmt.common.NotFoundException;
import cz.komercpoj.tmpmgmt.outbox.OutboxWriter;
import cz.komercpoj.tmpmgmt.template.application.TemplateCommands.Archive;
import cz.komercpoj.tmpmgmt.template.application.TemplateCommands.CreateTemplate;
import cz.komercpoj.tmpmgmt.template.application.TemplateCommands.PublishVersion;
import cz.komercpoj.tmpmgmt.template.application.TemplateCommands.SaveDraft;
import cz.komercpoj.tmpmgmt.template.application.TemplateCommands.UpdateMetadata;
import cz.komercpoj.tmpmgmt.template.application.events.TemplateEvents;
import cz.komercpoj.tmpmgmt.template.domain.TemplateStatus;
import cz.komercpoj.tmpmgmt.template.persistence.TemplateDraftEntity;
import cz.komercpoj.tmpmgmt.template.persistence.TemplateDraftRepository;
import cz.komercpoj.tmpmgmt.template.persistence.TemplateEntity;
import cz.komercpoj.tmpmgmt.template.persistence.TemplateRepository;
import cz.komercpoj.tmpmgmt.template.persistence.TemplateVersionEntity;
import cz.komercpoj.tmpmgmt.template.persistence.TemplateVersionRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TemplateService {

  private final TemplateRepository templates;
  private final TemplateDraftRepository drafts;
  private final TemplateVersionRepository versions;
  private final TemplateContentValidator validator;
  private final OutboxWriter outbox;
  private final ObjectMapper mapper;

  public TemplateService(
      TemplateRepository templates,
      TemplateDraftRepository drafts,
      TemplateVersionRepository versions,
      TemplateContentValidator validator,
      OutboxWriter outbox,
      ObjectMapper mapper) {
    this.templates = templates;
    this.drafts = drafts;
    this.versions = versions;
    this.validator = validator;
    this.outbox = outbox;
    this.mapper = mapper;
  }

  @Transactional
  public TemplateEntity create(CreateTemplate cmd) {
    if (templates.existsBySlug(cmd.slug())) {
      throw new ConflictException("template.slug_taken", "Slug already in use: " + cmd.slug());
    }
    UUID id = UUID.randomUUID();
    TemplateEntity t =
        TemplateEntity.newActive(
            id, cmd.slug(), cmd.name(), cmd.description(), cmd.category(), cmd.ownerUserId());
    templates.save(t);

    // Seed an empty draft so editors can open the template immediately.
    JsonNode emptyDoc =
        mapper
            .createObjectNode()
            .put("type", "doc")
            .set(
                "content",
                mapper.createArrayNode().add(mapper.createObjectNode().put("type", "paragraph")));
    JsonNode emptySchema =
        mapper
            .createObjectNode()
            .put("$schema", "https://json-schema.org/draft/2020-12/schema")
            .put("type", "object");
    drafts.save(TemplateDraftEntity.empty(id, emptyDoc, emptySchema, cmd.ownerUserId()));

    outbox.stage(
        TemplateEvents.AGGREGATE_TYPE,
        id.toString(),
        TemplateEvents.TYPE_CREATED,
        new TemplateEvents.TemplateCreated(
            id,
            cmd.slug(),
            cmd.name(),
            cmd.description(),
            cmd.category(),
            List.of(t.getTags()),
            cmd.ownerUserId(),
            Instant.now()));
    return t;
  }

  @Transactional(readOnly = true)
  public TemplateEntity getById(UUID id) {
    return templates.findById(id).orElseThrow(() -> notFound(id));
  }

  @Transactional(readOnly = true)
  public List<TemplateEntity> list() {
    return templates.findAll(Sort.by(Sort.Direction.DESC, "updatedAt"));
  }

  @Transactional(readOnly = true)
  public TemplateDraftEntity getDraft(UUID templateId) {
    return drafts
        .findById(templateId)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "template.draft_not_found", "No draft for template " + templateId));
  }

  @Transactional(readOnly = true)
  public List<TemplateVersionEntity> listVersions(UUID templateId) {
    // Ensure the template exists (404 vs empty list).
    getById(templateId);
    return versions.findByTemplateIdOrderByVersionNumberDesc(templateId);
  }

  @Transactional
  public TemplateEntity updateMetadata(UpdateMetadata cmd) {
    TemplateEntity template = getById(cmd.templateId());
    if (template.getStatus() == TemplateStatus.ARCHIVED) {
      throw new ConflictException(
          "template.archived", "Cannot edit archived template " + template.getId());
    }
    template.setName(cmd.name());
    template.setDescription(cmd.description());
    template.setCategory(cmd.category());
    template.setTags(cmd.tags() == null ? new String[0] : cmd.tags().toArray(new String[0]));
    template.touchUpdated();

    outbox.stage(
        TemplateEvents.AGGREGATE_TYPE,
        template.getId().toString(),
        TemplateEvents.TYPE_METADATA_UPDATED,
        new TemplateEvents.TemplateMetadataUpdated(
            template.getId(),
            template.getName(),
            template.getDescription(),
            template.getCategory(),
            List.of(template.getTags()),
            cmd.actorUserId(),
            Instant.now()));
    return template;
  }

  @Transactional
  public TemplateDraftEntity saveDraft(SaveDraft cmd) {
    TemplateEntity template = getById(cmd.templateId());
    if (template.getStatus() == TemplateStatus.ARCHIVED) {
      throw new ConflictException(
          "template.archived", "Cannot edit archived template " + template.getId());
    }

    validator.validate(cmd.content(), cmd.variablesSchema());

    TemplateDraftEntity draft =
        drafts
            .findById(cmd.templateId())
            .orElseGet(
                () ->
                    TemplateDraftEntity.empty(
                        cmd.templateId(),
                        cmd.content(),
                        cmd.variablesSchema(),
                        cmd.editorUserId()));
    draft.replace(cmd.content(), cmd.variablesSchema(), cmd.editorUserId());
    drafts.save(draft);

    template.touchUpdated();

    outbox.stage(
        TemplateEvents.AGGREGATE_TYPE,
        template.getId().toString(),
        TemplateEvents.TYPE_DRAFT_SAVED,
        new TemplateEvents.TemplateDraftSaved(template.getId(), cmd.editorUserId(), Instant.now()));
    return draft;
  }

  @Transactional
  public TemplateVersionEntity publishVersion(PublishVersion cmd) {
    TemplateEntity template = getById(cmd.templateId());
    if (template.getStatus() == TemplateStatus.ARCHIVED) {
      throw new ConflictException(
          "template.archived", "Cannot publish archived template " + template.getId());
    }
    TemplateDraftEntity draft = getDraft(cmd.templateId());

    validator.validate(draft.getContent(), draft.getVariablesSchema());

    int nextVersion = versions.findMaxVersionNumber(template.getId()) + 1;
    UUID versionId = UUID.randomUUID();
    TemplateVersionEntity v =
        TemplateVersionEntity.publish(
            versionId,
            template.getId(),
            nextVersion,
            draft.getContent(),
            draft.getVariablesSchema(),
            cmd.changeNote(),
            cmd.publishedBy());
    versions.save(v);

    template.touchUpdated();

    outbox.stage(
        TemplateEvents.AGGREGATE_TYPE,
        template.getId().toString(),
        TemplateEvents.TYPE_VERSION_PUBLISHED,
        new TemplateEvents.TemplateVersionPublished(
            template.getId(),
            versionId,
            nextVersion,
            cmd.changeNote(),
            cmd.publishedBy(),
            Instant.now()));
    return v;
  }

  @Transactional
  public void archive(Archive cmd) {
    TemplateEntity template = getById(cmd.templateId());
    if (template.getStatus() == TemplateStatus.ARCHIVED) {
      return; // idempotent
    }
    template.setStatus(TemplateStatus.ARCHIVED);
    template.touchUpdated();

    outbox.stage(
        TemplateEvents.AGGREGATE_TYPE,
        template.getId().toString(),
        TemplateEvents.TYPE_ARCHIVED,
        new TemplateEvents.TemplateArchived(template.getId(), cmd.actorUserId(), Instant.now()));
  }

  private NotFoundException notFound(UUID id) {
    return new NotFoundException("template.not_found", "Template not found: " + id);
  }
}
