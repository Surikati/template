package cz.komercpoj.tmpmgmt.template.api.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record UpdateMetadataRequest(
        @NotBlank String name,
        String description,
        String category,
        List<String> tags) {}
