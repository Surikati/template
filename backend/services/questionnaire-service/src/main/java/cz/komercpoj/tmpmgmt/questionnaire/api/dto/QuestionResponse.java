package cz.komercpoj.tmpmgmt.questionnaire.api.dto;

import tools.jackson.databind.JsonNode;
import cz.komercpoj.tmpmgmt.questionnaire.domain.QuestionType;
import java.util.UUID;

public record QuestionResponse(
    UUID id,
    int ordinal,
    String variablePath,
    String label,
    QuestionType questionType,
    JsonNode validation,
    String visibilityRule,
    JsonNode options) {}
