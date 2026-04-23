package cz.komercpoj.tmpmgmt.questionnaire.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.UUID;

public record CreateQuestionnaireRequest(
        @NotNull UUID templateId,
        @Positive int templateVersionNumber,
        @NotBlank String name,
        @NotNull @Valid List<SectionInputDto> sections) {}
