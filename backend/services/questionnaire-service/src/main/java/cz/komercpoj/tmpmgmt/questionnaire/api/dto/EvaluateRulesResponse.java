package cz.komercpoj.tmpmgmt.questionnaire.api.dto;

import java.util.Map;

public record EvaluateRulesResponse(Map<String, RuleResult> results) {

  /** Per-rule outcome — {@code value} reflects truthiness; {@code error} is null on success. */
  public record RuleResult(boolean value, String error) {}
}
