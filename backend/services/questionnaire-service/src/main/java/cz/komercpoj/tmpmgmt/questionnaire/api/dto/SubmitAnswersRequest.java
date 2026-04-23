package cz.komercpoj.tmpmgmt.questionnaire.api.dto;

import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record SubmitAnswersRequest(@NotNull Map<String, Object> answers) {}
