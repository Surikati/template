package cz.komercpoj.tmpmgmt.questionnaire.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.komercpoj.tmpmgmt.questionnaire.TestSecurityConfig;
import cz.komercpoj.tmpmgmt.questionnaire.TestcontainersConfig;
import cz.komercpoj.tmpmgmt.questionnaire.persistence.QuestionnaireVersionEntity;
import cz.komercpoj.tmpmgmt.questionnaire.persistence.QuestionnaireVersionRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import({TestcontainersConfig.class, TestSecurityConfig.class})
class QuestionnaireVersionPruningIT {

  @Autowired QuestionnaireVersionRepository versions;
  @Autowired QuestionnaireVersionPruningJob job;
  @Autowired JdbcTemplate jdbc;
  @Autowired ObjectMapper mapper;

  @Test
  void deletesOldVersions_butKeepsLatestPerQuestionnaire() {
    UUID q1 = seedQuestionnaire("Q1");
    UUID q2 = seedQuestionnaire("Q2");

    Instant longAgo = Instant.now().minus(120, ChronoUnit.DAYS);
    Instant recent = Instant.now().minus(10, ChronoUnit.DAYS);

    UUID q1v1Id = seedVersion(q1, 1, longAgo);
    UUID q1v2Id = seedVersion(q1, 2, longAgo);
    UUID q1v3Id = seedVersion(q1, 3, recent);

    UUID q2v1Id = seedVersion(q2, 1, longAgo); // single version, should survive

    var result = job.run();

    assertThat(result.deleted()).isEqualTo(2);

    List<UUID> remaining =
        versions.findAll().stream().map(QuestionnaireVersionEntity::getId).toList();
    assertThat(remaining).containsExactlyInAnyOrder(q1v3Id, q2v1Id);
    assertThat(remaining).doesNotContain(q1v1Id, q1v2Id);
  }

  @Test
  void noOpWhenAllVersionsAreFreshOrSingleton() {
    UUID q = seedQuestionnaire("Solo");
    Instant longAgo = Instant.now().minus(200, ChronoUnit.DAYS);
    UUID only = seedVersion(q, 1, longAgo); // singleton, must survive

    var result = job.run();

    assertThat(result.deleted()).isZero();
    assertThat(versions.findById(only)).isPresent();
  }

  /** Inserts a parent {@code questionnaire} row directly via JdbcTemplate. */
  private UUID seedQuestionnaire(String name) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO questionnaire (id, template_id, template_version_number, name)"
            + " VALUES (?, ?, ?, ?)",
        id,
        UUID.randomUUID(),
        1,
        name);
    return id;
  }

  private UUID seedVersion(UUID questionnaireId, int versionNumber, Instant publishedAt) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO questionnaire_version"
            + " (id, questionnaire_id, version_number, name_snapshot, structure_snapshot,"
            + "  published_at, published_by)"
            + " VALUES (?, ?, ?, ?, '{}'::jsonb, ?, ?)",
        id,
        questionnaireId,
        versionNumber,
        "snap",
        java.sql.Timestamp.from(publishedAt),
        UUID.randomUUID());
    return id;
  }
}
