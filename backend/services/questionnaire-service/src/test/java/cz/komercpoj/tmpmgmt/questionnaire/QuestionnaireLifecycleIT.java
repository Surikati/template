package cz.komercpoj.tmpmgmt.questionnaire;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.komercpoj.tmpmgmt.common.ConflictException;
import cz.komercpoj.tmpmgmt.questionnaire.application.QuestionnaireCommands;
import cz.komercpoj.tmpmgmt.questionnaire.application.QuestionnaireService;
import cz.komercpoj.tmpmgmt.questionnaire.application.SessionService;
import cz.komercpoj.tmpmgmt.questionnaire.domain.QuestionType;
import cz.komercpoj.tmpmgmt.questionnaire.domain.SessionState;
import cz.komercpoj.tmpmgmt.questionnaire.persistence.QuestionnaireEntity;
import java.util.List;
import java.util.Map;
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
class QuestionnaireLifecycleIT {

    @Autowired QuestionnaireService questionnaires;
    @Autowired SessionService sessions;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper mapper;

    private final UUID actor = UUID.randomUUID();

    @Test
    void fullLifecycle_structure_and_session() {
        UUID templateId = UUID.randomUUID();

        // 1. Create questionnaire with 2 sections
        var created = questionnaires.create(new QuestionnaireCommands.CreateQuestionnaire(
                templateId,
                1,
                "NDA Questionnaire",
                List.of(
                        new QuestionnaireCommands.SectionInput(
                                0, "Strany smlouvy", null,
                                List.of(
                                        new QuestionnaireCommands.QuestionInput(
                                                0, "client.name", "Název klienta",
                                                QuestionType.TEXT, null, null, null),
                                        new QuestionnaireCommands.QuestionInput(
                                                1, "client.ico", "IČO",
                                                QuestionType.TEXT, null, null, null))),
                        new QuestionnaireCommands.SectionInput(
                                1, "Podmínky", null,
                                List.of(
                                        new QuestionnaireCommands.QuestionInput(
                                                0, "terms.duration_months", "Trvání (měsíce)",
                                                QuestionType.NUMBER, null, null, null))))));
        assertThat(created.getSections()).hasSize(2);
        assertThat(created.getSections().get(0).getQuestions()).hasSize(2);

        // 2. Replace structure (simulates editor save)
        var updated = questionnaires.replaceStructure(new QuestionnaireCommands.ReplaceStructure(
                created.getId(),
                "NDA Questionnaire (v2)",
                List.of(new QuestionnaireCommands.SectionInput(
                        0, "Jen klient", null,
                        List.of(new QuestionnaireCommands.QuestionInput(
                                0, "client.name", "Název klienta",
                                QuestionType.TEXT, null, null, null))))));
        assertThat(updated.getName()).isEqualTo("NDA Questionnaire (v2)");
        assertThat(updated.getSections()).hasSize(1);
        assertThat(updated.getSections().get(0).getQuestions()).hasSize(1);

        // 3. Start a session
        var session = sessions.start(new QuestionnaireCommands.StartSession(created.getId(), actor));
        assertThat(session.getState()).isEqualTo(SessionState.IN_PROGRESS);

        // 4. Submit partial answers (no outbox event — intentional)
        sessions.submitAnswers(new QuestionnaireCommands.SubmitAnswers(
                session.getId(), Map.of("client.name", "ACME s.r.o.")));

        // 5. Complete session
        var completed = sessions.complete(new QuestionnaireCommands.CompleteSession(session.getId()));
        assertThat(completed.getState()).isEqualTo(SessionState.COMPLETED);
        assertThat(completed.getCompletedAt()).isNotNull();

        // 6. Outbox — structure events + session events, answers-only events skipped
        List<Map<String, Object>> structureEvents = jdbc.queryForList(
                "SELECT event_type FROM outbox_event WHERE aggregate_type = 'questionnaire' "
                        + "AND aggregate_id = ? ORDER BY occurred_at",
                created.getId().toString());
        assertThat(structureEvents).extracting(r -> r.get("event_type"))
                .containsExactly("created", "updated");

        List<Map<String, Object>> sessionEvents = jdbc.queryForList(
                "SELECT event_type FROM outbox_event WHERE aggregate_type = 'questionnaire_session' "
                        + "AND aggregate_id = ? ORDER BY occurred_at",
                session.getId().toString());
        assertThat(sessionEvents).extracting(r -> r.get("event_type"))
                .containsExactly("started", "completed");
    }

    @Test
    void createDuplicateForSameTemplateVersion_throwsConflict() {
        UUID templateId = UUID.randomUUID();
        questionnaires.create(new QuestionnaireCommands.CreateQuestionnaire(
                templateId, 1, "Q1", List.of()));
        assertThatThrownBy(() -> questionnaires.create(new QuestionnaireCommands.CreateQuestionnaire(
                templateId, 1, "Q2", List.of())))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void submitAfterComplete_throwsConflict() {
        UUID templateId = UUID.randomUUID();
        QuestionnaireEntity q = questionnaires.create(new QuestionnaireCommands.CreateQuestionnaire(
                templateId, 1, "Q", List.of()));
        var session = sessions.start(new QuestionnaireCommands.StartSession(q.getId(), actor));
        sessions.complete(new QuestionnaireCommands.CompleteSession(session.getId()));
        assertThatThrownBy(() -> sessions.submitAnswers(new QuestionnaireCommands.SubmitAnswers(
                session.getId(), Map.of("foo", "bar"))))
                .isInstanceOf(ConflictException.class);
    }
}
