package cz.komercpoj.tmpmgmt.questionnaire.application.events;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

public final class QuestionnaireEvents {

    private QuestionnaireEvents() {}

    public static final String AGGREGATE_TYPE = "questionnaire";
    public static final String SESSION_AGGREGATE_TYPE = "questionnaire_session";

    public static final String TYPE_CREATED = "created";
    public static final String TYPE_UPDATED = "updated";
    public static final String TYPE_SESSION_STARTED = "started";
    public static final String TYPE_SESSION_COMPLETED = "completed";

    public record QuestionnaireCreated(
            UUID questionnaireId,
            UUID templateId,
            int templateVersionNumber,
            String name,
            int sectionCount,
            Instant occurredAt) {}

    public record QuestionnaireUpdated(
            UUID questionnaireId, int sectionCount, Instant occurredAt) {}

    public record SessionStarted(
            UUID sessionId, UUID questionnaireId, UUID startedBy, Instant occurredAt) {}

    public record SessionCompleted(
            UUID sessionId,
            UUID questionnaireId,
            UUID startedBy,
            JsonNode answers,
            Instant occurredAt) {}
}
