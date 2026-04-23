package cz.komercpoj.tmpmgmt.questionnaire.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ReplaceStructureRequest(
        @NotBlank String name, @NotNull @Valid List<SectionInputDto> sections) {}
