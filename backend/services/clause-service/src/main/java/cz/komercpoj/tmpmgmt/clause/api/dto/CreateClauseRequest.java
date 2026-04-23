package cz.komercpoj.tmpmgmt.clause.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateClauseRequest(
        @NotBlank @Size(max = 200) @Pattern(regexp = "[a-z0-9][a-z0-9-]*") String slug,
        @NotBlank @Size(max = 500) String name,
        @Size(max = 5000) String description,
        @Size(max = 100) String category) {}
