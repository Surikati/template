package cz.komercpoj.tmpmgmt.questionnaire.application;

import com.fasterxml.jackson.databind.JsonNode;
import cz.komercpoj.tmpmgmt.questionnaire.domain.QuestionType;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class QuestionnaireCommands {

    private QuestionnaireCommands() {}

    public record QuestionInput(
            int ordinal,
            String variablePath,
            String label,
            QuestionType questionType,
            JsonNode validation,
            String visibilityRule,
            JsonNode options) {}

    public record SectionInput(
            int ordinal, String title, String visibilityRule, List<QuestionInput> questions) {}

    public record CreateQuestionnaire(
            UUID templateId, int templateVersionNumber, String name, List<SectionInput> sections) {}

    public record ReplaceStructure(UUID questionnaireId, String name, List<SectionInput> sections) {}

    public record PublishQuestionnaireVersion(UUID questionnaireId, UUID publishedBy) {}

    public record StartSession(UUID questionnaireId, UUID startedBy) {}

    public record SubmitAnswers(UUID sessionId, Map<String, Object> answers) {}

    public record CompleteSession(UUID sessionId) {}
}
