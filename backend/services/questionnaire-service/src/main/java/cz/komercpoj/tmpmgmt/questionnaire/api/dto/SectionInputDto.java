package cz.komercpoj.tmpmgmt.questionnaire.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;

public record SectionInputDto(
        @PositiveOrZero int ordinal,
        @NotBlank String title,
        String visibilityRule,
        @NotNull @Valid List<QuestionInputDto> questions) {}
