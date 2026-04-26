package cz.komercpoj.tmpmgmt.template;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import cz.komercpoj.tmpmgmt.common.ConflictException;
import cz.komercpoj.tmpmgmt.template.application.TemplateCommands;
import cz.komercpoj.tmpmgmt.template.application.TemplateService;
import cz.komercpoj.tmpmgmt.template.domain.TemplateStatus;
import cz.komercpoj.tmpmgmt.template.persistence.TemplateVersionEntity;
import cz.komercpoj.tmpmgmt.template.persistence.TemplateVersionRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * End-to-end verification: template lifecycle exercises JPA, validation, outbox staging, and the
 * DB-level immutability trigger on {@code template_version}.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import({TestcontainersConfig.class, TestSecurityConfig.class})
class TemplateLifecycleIT {

  @Autowired TemplateService service;
  @Autowired TemplateVersionRepository versions;
  @Autowired JdbcTemplate jdbc;
  @Autowired ObjectMapper mapper;
  @Autowired TransactionTemplate tx;

  private final UUID actor = UUID.randomUUID();

  @Test
  void fullLifecycle_stagesExpectedEvents_andEnforcesVersionImmutability() {
    // 1. Create
    var created =
        service.create(
            new TemplateCommands.CreateTemplate(
                "nda-standard", "NDA — standard", "Mlčenlivost", "legal", actor));
    UUID templateId = created.getId();
    assertThat(created.getStatus()).isEqualTo(TemplateStatus.ACTIVE);

    // 2. Save draft with real content
    ObjectNode doc = mapper.createObjectNode().put("type", "doc");
    doc.set(
        "content",
        mapper
            .createArrayNode()
            .add(
                mapper
                    .createObjectNode()
                    .put("type", "paragraph")
                    .set(
                        "content",
                        mapper
                            .createArrayNode()
                            .add(
                                mapper
                                    .createObjectNode()
                                    .put("type", "text")
                                    .put("text", "Hello ")))));
    ObjectNode schema =
        mapper
            .createObjectNode()
            .put("$schema", "https://json-schema.org/draft/2020-12/schema")
            .put("type", "object");
    service.saveDraft(new TemplateCommands.SaveDraft(templateId, doc, schema, actor));

    // 3. Publish version 1
    var v1 =
        service.publishVersion(
            new TemplateCommands.PublishVersion(templateId, "initial release", actor));
    assertThat(v1.getVersionNumber()).isEqualTo(1);

    // 4. Publish version 2 (after another draft change)
    ObjectNode doc2 = doc.deepCopy();
    doc2.put("foo", "bar"); // trivial change
    service.saveDraft(new TemplateCommands.SaveDraft(templateId, doc2, schema, actor));
    var v2 =
        service.publishVersion(new TemplateCommands.PublishVersion(templateId, "tweak", actor));
    assertThat(v2.getVersionNumber()).isEqualTo(2);

    // 5. Versions are listable and ordered desc
    List<TemplateVersionEntity> list = service.listVersions(templateId);
    assertThat(list).extracting(TemplateVersionEntity::getVersionNumber).containsExactly(2, 1);

    // 6. Outbox has the expected sequence of events
    List<Map<String, Object>> outboxRows =
        jdbc.queryForList(
            "SELECT event_type FROM outbox_event WHERE aggregate_id = ? ORDER BY occurred_at",
            templateId.toString());
    assertThat(outboxRows)
        .extracting(r -> r.get("event_type"))
        .containsExactly(
            "created", "draft.saved", "version.published", "draft.saved", "version.published");

    // 7. DB trigger rejects UPDATE on template_version — the core immutability guarantee
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "UPDATE template_version SET change_note = 'hacked' WHERE id = ?", v1.getId()))
        .isInstanceOfAny(DataIntegrityViolationException.class, RuntimeException.class);
  }

  @Test
  void createWithDuplicateSlug_throwsConflict() {
    service.create(
        new TemplateCommands.CreateTemplate("contract-v1", "Kontrakt", null, "legal", actor));
    assertThatThrownBy(
            () ->
                service.create(
                    new TemplateCommands.CreateTemplate(
                        "contract-v1", "Jiný kontrakt", null, "legal", actor)))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  void publishVersion_withUnchangedContent_isIdempotent() {
    var t =
        service.create(
            new TemplateCommands.CreateTemplate(
                "idempotent-pub", "Idempotent", null, "legal", actor));
    UUID templateId = t.getId();

    ObjectNode doc =
        mapper
            .createObjectNode()
            .put("type", "doc")
            .set(
                "content",
                mapper.createArrayNode().add(mapper.createObjectNode().put("type", "paragraph")));
    ObjectNode schema =
        mapper
            .createObjectNode()
            .put("$schema", "https://json-schema.org/draft/2020-12/schema")
            .put("type", "object");
    service.saveDraft(new TemplateCommands.SaveDraft(templateId, doc, schema, actor));

    var v1 =
        service.publishVersion(new TemplateCommands.PublishVersion(templateId, "first", actor));
    assertThat(v1.getVersionNumber()).isEqualTo(1);

    // Second publish without modifying the draft must return the same version row,
    // not bump the number, and not stage a new outbox event.
    var v1Again =
        service.publishVersion(
            new TemplateCommands.PublishVersion(templateId, "ignored note", actor));
    assertThat(v1Again.getId()).isEqualTo(v1.getId());
    assertThat(v1Again.getVersionNumber()).isEqualTo(1);
    assertThat(versions.findByTemplateIdOrderByVersionNumberDesc(templateId)).hasSize(1);

    Long publishedEvents =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM outbox_event WHERE aggregate_id = ? AND event_type = 'version.published'",
            Long.class,
            templateId.toString());
    assertThat(publishedEvents).isEqualTo(1L);

    // Editing the draft and publishing again must produce v2.
    ObjectNode doc2 = doc.deepCopy();
    doc2.put("foo", "bar");
    service.saveDraft(new TemplateCommands.SaveDraft(templateId, doc2, schema, actor));
    var v2 =
        service.publishVersion(
            new TemplateCommands.PublishVersion(templateId, "real change", actor));
    assertThat(v2.getVersionNumber()).isEqualTo(2);
  }

  @Test
  void diffVersions_returnsBothSnapshots_andFlagsContentChange() {
    var t =
        service.create(
            new TemplateCommands.CreateTemplate("diffable", "Diffable", null, "legal", actor));
    UUID templateId = t.getId();
    ObjectNode schema = mapper.createObjectNode().put("type", "object");

    ObjectNode v1Doc = mapper.createObjectNode().put("type", "doc").put("rev", 1);
    service.saveDraft(new TemplateCommands.SaveDraft(templateId, v1Doc, schema, actor));
    var v1 =
        service.publishVersion(new TemplateCommands.PublishVersion(templateId, "first", actor));

    ObjectNode v2Doc = mapper.createObjectNode().put("type", "doc").put("rev", 2);
    service.saveDraft(new TemplateCommands.SaveDraft(templateId, v2Doc, schema, actor));
    var v2 =
        service.publishVersion(new TemplateCommands.PublishVersion(templateId, "second", actor));

    var diff = service.diffVersions(templateId, v1.getVersionNumber(), v2.getVersionNumber());
    assertThat(diff.from().getVersionNumber()).isEqualTo(1);
    assertThat(diff.to().getVersionNumber()).isEqualTo(2);
    assertThat(diff.from().getContent().get("rev").asInt()).isEqualTo(1);
    assertThat(diff.to().getContent().get("rev").asInt()).isEqualTo(2);
    assertThat(diff.from().getContent()).isNotEqualTo(diff.to().getContent());
    assertThat(diff.from().getVariablesSchema()).isEqualTo(diff.to().getVariablesSchema());
  }

  @Test
  void diffVersions_unknownVersion_throwsNotFound() {
    var t =
        service.create(
            new TemplateCommands.CreateTemplate("diff-404", "404", null, "legal", actor));
    UUID templateId = t.getId();

    assertThatThrownBy(() -> service.diffVersions(templateId, 1, 2))
        .isInstanceOf(cz.komercpoj.tmpmgmt.common.NotFoundException.class)
        .hasMessageContaining("version_not_found");
  }

  @Test
  void exportImport_roundTrip_preservesContentAndVersions() {
    // 1. Build a template with two published versions and a draft on top
    var t =
        service.create(
            new TemplateCommands.CreateTemplate(
                "exportable", "Exportable", "Export source", "legal", actor));
    UUID sourceId = t.getId();
    ObjectNode schema = mapper.createObjectNode().put("type", "object");

    ObjectNode v1Doc = mapper.createObjectNode().put("type", "doc").put("rev", 1);
    service.saveDraft(new TemplateCommands.SaveDraft(sourceId, v1Doc, schema, actor));
    service.publishVersion(new TemplateCommands.PublishVersion(sourceId, "v1", actor));

    ObjectNode v2Doc = mapper.createObjectNode().put("type", "doc").put("rev", 2);
    service.saveDraft(new TemplateCommands.SaveDraft(sourceId, v2Doc, schema, actor));
    service.publishVersion(new TemplateCommands.PublishVersion(sourceId, "v2", actor));

    ObjectNode draftDoc = mapper.createObjectNode().put("type", "doc").put("rev", 3);
    service.saveDraft(new TemplateCommands.SaveDraft(sourceId, draftDoc, schema, actor));

    // 2. Re-import as a new template under a fresh slug
    var bundleVersions =
        service.listVersions(sourceId).stream()
            .sorted((a, b) -> Integer.compare(a.getVersionNumber(), b.getVersionNumber()))
            .map(
                v ->
                    new TemplateCommands.ImportedVersion(
                        v.getVersionNumber(),
                        v.getContent(),
                        v.getVariablesSchema(),
                        v.getChangeNote()))
            .toList();
    var draft = service.getDraft(sourceId);
    var imported =
        service.importBundle(
            new TemplateCommands.ImportBundle(
                "exportable-clone",
                "Exportable Clone",
                "Cloned from export",
                "legal",
                List.of("imported"),
                draft.getContent(),
                draft.getVariablesSchema(),
                bundleVersions,
                actor));

    // 3. Imported template carries the same versions and draft
    assertThat(imported.getId()).isNotEqualTo(sourceId);
    assertThat(imported.getSlug()).isEqualTo("exportable-clone");
    assertThat(service.listVersions(imported.getId()))
        .extracting(v -> v.getVersionNumber())
        .containsExactly(2, 1);
    assertThat(service.listVersions(imported.getId()))
        .anySatisfy(
            v -> assertThat(v.getContent().get("rev").asInt()).isEqualTo(v.getVersionNumber()));
    assertThat(service.getDraft(imported.getId()).getContent().get("rev").asInt()).isEqualTo(3);
  }

  @Test
  void importBundle_withTakenSlug_throwsConflict() {
    service.create(new TemplateCommands.CreateTemplate("taken", "Taken", null, null, actor));

    assertThatThrownBy(
            () ->
                service.importBundle(
                    new TemplateCommands.ImportBundle(
                        "taken",
                        "Different Name",
                        null,
                        null,
                        null,
                        mapper.createObjectNode(),
                        mapper.createObjectNode(),
                        List.of(),
                        actor)))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  void publishOnArchivedTemplate_throwsConflict() {
    var t =
        service.create(
            new TemplateCommands.CreateTemplate("to-archive", "Archiv", null, null, actor));
    service.archive(new TemplateCommands.Archive(t.getId(), actor));
    assertThatThrownBy(
            () ->
                service.publishVersion(
                    new TemplateCommands.PublishVersion(t.getId(), "should fail", actor)))
        .isInstanceOf(ConflictException.class);
  }
}
