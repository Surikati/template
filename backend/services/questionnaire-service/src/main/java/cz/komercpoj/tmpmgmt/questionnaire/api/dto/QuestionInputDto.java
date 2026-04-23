package cz.komercpoj.tmpmgmt.questionnaire.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import cz.komercpoj.tmpmgmt.questionnaire.domain.QuestionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record QuestionInputDto(
        @PositiveOrZero int ordinal,
        @NotBlank String variablePath,
        @NotBlank String label,
        @NotNull QuestionType questionType,
        JsonNode validation,
        String visibilityRule,
        JsonNode options) {}
