package cz.komercpoj.tmpmgmt.clause;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import cz.komercpoj.tmpmgmt.clause.application.ClauseCommands;
import cz.komercpoj.tmpmgmt.clause.application.ClauseService;
import cz.komercpoj.tmpmgmt.clause.domain.ClauseStatus;
import cz.komercpoj.tmpmgmt.clause.persistence.ClauseVersionEntity;
import cz.komercpoj.tmpmgmt.common.ConflictException;
import cz.komercpoj.tmpmgmt.common.ValidationException;
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

@SpringBootTest
@ActiveProfiles("test")
@Import({TestcontainersConfig.class, TestSecurityConfig.class})
class ClauseLifecycleIT {

  @Autowired ClauseService service;
  @Autowired JdbcTemplate jdbc;
  @Autowired ObjectMapper mapper;

  private final UUID actor = UUID.randomUUID();

  @Test
  void fullLifecycle_stagesExpectedEvents_andEnforcesVersionImmutability() {
    // 1. Create
    var created =
        service.create(
            new ClauseCommands.CreateClause(
                "gdpr-standard", "GDPR — standard", "Zpracování osobních údajů", "legal", actor));
    UUID clauseId = created.getId();
    assertThat(created.getStatus()).isEqualTo(ClauseStatus.ACTIVE);

    // 2. Publish v1
    ObjectNode content = mapper.createObjectNode().put("type", "fragment");
    content.set(
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
                                    .put("text", "Zpracování osobních údajů v souladu s GDPR.")))));
    var v1 =
        service.publishVersion(
            new ClauseCommands.PublishVersion(clauseId, content, "initial release", actor));
    assertThat(v1.getVersionNumber()).isEqualTo(1);

    // 3. Publish v2
    ObjectNode content2 = content.deepCopy();
    content2.put("revision", 2);
    var v2 =
        service.publishVersion(
            new ClauseCommands.PublishVersion(clauseId, content2, "minor update", actor));
    assertThat(v2.getVersionNumber()).isEqualTo(2);

    // 4. Versions listed desc
    List<ClauseVersionEntity> list = service.listVersions(clauseId);
    assertThat(list).extracting(ClauseVersionEntity::getVersionNumber).containsExactly(2, 1);

    // 5. Outbox events in order
    List<Map<String, Object>> rows =
        jdbc.queryForList(
            "SELECT event_type FROM outbox_event WHERE aggregate_id = ? ORDER BY occurred_at",
            clauseId.toString());
    assertThat(rows)
        .extracting(r -> r.get("event_type"))
        .containsExactly("created", "version.published", "version.published");

    // 6. Immutability trigger — UPDATE must fail
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "UPDATE clause_version SET change_note = 'hacked' WHERE id = ?", v1.getId()))
        .isInstanceOfAny(DataIntegrityViolationException.class, RuntimeException.class);
  }

  @Test
  void createWithDuplicateSlug_throwsConflict() {
    service.create(
        new ClauseCommands.CreateClause("liability", "Odpovědnost", null, "legal", actor));
    assertThatThrownBy(
            () ->
                service.create(
                    new ClauseCommands.CreateClause(
                        "liability", "Jiná doložka", null, "legal", actor)))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  void publishWithInvalidContent_throwsValidation() {
    var c =
        service.create(new ClauseCommands.CreateClause("bad-content", "Bad", null, null, actor));
    // Root is not a 'fragment' — validator rejects it.
    ObjectNode wrong = mapper.createObjectNode().put("type", "doc");
    wrong.set("content", mapper.createArrayNode());
    assertThatThrownBy(
            () ->
                service.publishVersion(
                    new ClauseCommands.PublishVersion(c.getId(), wrong, null, actor)))
        .isInstanceOf(ValidationException.class);
  }

  @Test
  void publishOnArchivedClause_throwsConflict() {
    var c =
        service.create(new ClauseCommands.CreateClause("to-archive", "Archiv", null, null, actor));
    service.archive(new ClauseCommands.Archive(c.getId(), actor));
    ObjectNode content = mapper.createObjectNode().put("type", "fragment");
    content.set("content", mapper.createArrayNode());
    assertThatThrownBy(
            () ->
                service.publishVersion(
                    new ClauseCommands.PublishVersion(c.getId(), content, null, actor)))
        .isInstanceOf(ConflictException.class);
  }
}
