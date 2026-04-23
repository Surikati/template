package cz.komercpoj.tmpmgmt.questionnaire.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import cz.komercpoj.tmpmgmt.common.ConflictException;
import cz.komercpoj.tmpmgmt.common.NotFoundException;
import cz.komercpoj.tmpmgmt.outbox.OutboxWriter;
import cz.komercpoj.tmpmgmt.questionnaire.application.QuestionnaireCommands.CompleteSession;
import cz.komercpoj.tmpmgmt.questionnaire.application.QuestionnaireCommands.StartSession;
import cz.komercpoj.tmpmgmt.questionnaire.application.QuestionnaireCommands.SubmitAnswers;
import cz.komercpoj.tmpmgmt.questionnaire.application.events.QuestionnaireEvents;
import cz.komercpoj.tmpmgmt.questionnaire.domain.SessionState;
import cz.komercpoj.tmpmgmt.questionnaire.persistence.QuestionnaireRepository;
import cz.komercpoj.tmpmgmt.questionnaire.persistence.QuestionnaireSessionEntity;
import cz.komercpoj.tmpmgmt.questionnaire.persistence.QuestionnaireSessionRepository;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionService {

    private final QuestionnaireRepository questionnaires;
    private final QuestionnaireSessionRepository sessions;
    private final OutboxWriter outbox;
    private final ObjectMapper mapper;

    public SessionService(
            QuestionnaireRepository questionnaires,
            QuestionnaireSessionRepository sessions,
            OutboxWriter outbox,
            ObjectMapper mapper) {
        this.questionnaires = questionnaires;
        this.sessions = sessions;
        this.outbox = outbox;
        this.mapper = mapper;
    }

    @Transactional
    public QuestionnaireSessionEntity start(StartSession cmd) {
        if (!questionnaires.existsById(cmd.questionnaireId())) {
            throw new NotFoundException(
                    "questionnaire.not_found",
                    "Questionnaire not found: " + cmd.questionnaireId());
        }
        UUID id = UUID.randomUUID();
        QuestionnaireSessionEntity s = QuestionnaireSessionEntity.start(
                id, cmd.questionnaireId(), cmd.startedBy(), mapper.createObjectNode());
        sessions.save(s);

        outbox.stage(
                QuestionnaireEvents.SESSION_AGGREGATE_TYPE,
                id.toString(),
                QuestionnaireEvents.TYPE_SESSION_STARTED,
                new QuestionnaireEvents.SessionStarted(
                        id, cmd.questionnaireId(), cmd.startedBy(), Instant.now()));
        return s;
    }

    @Transactional(readOnly = true)
    public QuestionnaireSessionEntity getById(UUID id) {
        return sessions.findById(id).orElseThrow(() -> sessionNotFound(id));
    }

    /**
     * Merges new answers into the session's answer map. No outbox event — mid-session updates are
     * intentionally silent to avoid event spam; only completion emits a final event with the full
     * answer payload.
     */
    @Transactional
    public QuestionnaireSessionEntity submitAnswers(SubmitAnswers cmd) {
        QuestionnaireSessionEntity s = getById(cmd.sessionId());
        if (s.getState() != SessionState.IN_PROGRESS) {
            throw new ConflictException(
                    "session.not_in_progress",
                    "Session is " + s.getState() + ", cannot submit answers.");
        }
        ObjectNode merged = s.getAnswers().deepCopy().isObject()
                ? ((ObjectNode) s.getAnswers().deepCopy())
                : mapper.createObjectNode();
        for (Map.Entry<String, Object> e : cmd.answers().entrySet()) {
            merged.set(e.getKey(), mapper.valueToTree(e.getValue()));
        }
        s.setAnswers(merged);
        return s;
    }

    @Transactional
    public QuestionnaireSessionEntity complete(CompleteSession cmd) {
        QuestionnaireSessionEntity s = getById(cmd.sessionId());
        if (s.getState() != SessionState.IN_PROGRESS) {
            throw new ConflictException(
                    "session.not_in_progress",
                    "Session is already " + s.getState() + ".");
        }
        s.markCompleted();

        outbox.stage(
                QuestionnaireEvents.SESSION_AGGREGATE_TYPE,
                s.getId().toString(),
                QuestionnaireEvents.TYPE_SESSION_COMPLETED,
                new QuestionnaireEvents.SessionCompleted(
                        s.getId(),
                        s.getQuestionnaireId(),
                        s.getStartedBy(),
                        s.getAnswers(),
                        Instant.now()));
        return s;
    }

    private NotFoundException sessionNotFound(UUID id) {
        return new NotFoundException("session.not_found", "Session not found: " + id);
    }
}
