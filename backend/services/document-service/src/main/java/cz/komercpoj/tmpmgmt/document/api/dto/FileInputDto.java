package cz.komercpoj.tmpmgmt.document.api.dto;

import cz.komercpoj.tmpmgmt.document.domain.FileFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FileInputDto(@NotNull FileFormat format, @NotBlank String contentBase64) {}
