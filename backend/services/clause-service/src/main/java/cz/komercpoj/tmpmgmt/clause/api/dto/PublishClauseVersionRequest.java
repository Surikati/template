package cz.komercpoj.tmpmgmt.clause.api.dto;

import tools.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PublishClauseVersionRequest(
    @NotNull JsonNode content, @Size(max = 5000) String changeNote) {}
