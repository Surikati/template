package cz.komercpoj.tmpmgmt.clause.api.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record UpdateMetadataRequest(
    @NotBlank String name, String description, String category, List<String> tags) {}
