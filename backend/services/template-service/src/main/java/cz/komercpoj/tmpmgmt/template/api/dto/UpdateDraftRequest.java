package cz.komercpoj.tmpmgmt.template.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;

public record UpdateDraftRequest(@NotNull JsonNode content, @NotNull JsonNode variablesSchema) {}
