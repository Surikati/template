package cz.komercpoj.tmpmgmt.rendering.api.dto;

import tools.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record RenderRequest(
    @NotNull JsonNode content, @NotNull Map<String, Object> data, @NotNull RenderFormat format) {}
