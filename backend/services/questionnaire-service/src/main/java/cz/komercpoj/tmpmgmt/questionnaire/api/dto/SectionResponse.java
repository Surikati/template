package cz.komercpoj.tmpmgmt.questionnaire.api.dto;

import java.util.List;
import java.util.UUID;

public record SectionResponse(
    UUID id, int ordinal, String title, String visibilityRule, List<QuestionResponse> questions) {}
