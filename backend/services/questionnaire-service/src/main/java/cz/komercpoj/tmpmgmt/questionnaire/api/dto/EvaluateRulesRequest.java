package cz.komercpoj.tmpmgmt.questionnaire.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

public record EvaluateRulesRequest(
    @NotNull @Valid List<RuleInput> rules, @NotNull Map<String, Object> context) {

  public record RuleInput(@NotBlank String key, @NotBlank String expression) {}
}
