package cz.komercpoj.tmpmgmt.questionnaire.api;

import cz.komercpoj.tmpmgmt.expression.ExpressionEvaluator;
import cz.komercpoj.tmpmgmt.questionnaire.api.dto.EvaluateRulesRequest;
import cz.komercpoj.tmpmgmt.questionnaire.api.dto.EvaluateRulesResponse;
import cz.komercpoj.tmpmgmt.questionnaire.api.dto.EvaluateRulesResponse.RuleResult;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Batched expression evaluator used by the questionnaire runner to compute section/question
 * visibility client-side without re-implementing the rule grammar in TypeScript.
 *
 * <p>Per-rule errors are captured and returned as {@code value=false, error=<msg>} so a single
 * malformed rule cannot break the entire runner.
 */
@RestController
@RequestMapping("/api/v1/questionnaires/evaluate-rules")
public class RuleEvaluationController {

  private final ExpressionEvaluator evaluator;

  public RuleEvaluationController(ExpressionEvaluator evaluator) {
    this.evaluator = evaluator;
  }

  @PostMapping
  public EvaluateRulesResponse evaluate(@Valid @RequestBody EvaluateRulesRequest req) {
    Map<String, RuleResult> results = new LinkedHashMap<>();
    for (var rule : req.rules()) {
      try {
        boolean value = evaluator.evaluateBoolean(rule.expression(), req.context());
        results.put(rule.key(), new RuleResult(value, null));
      } catch (RuntimeException e) {
        results.put(rule.key(), new RuleResult(false, e.getMessage()));
      }
    }
    return new EvaluateRulesResponse(results);
  }
}
