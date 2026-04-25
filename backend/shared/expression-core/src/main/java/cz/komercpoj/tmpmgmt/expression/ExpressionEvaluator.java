package cz.komercpoj.tmpmgmt.expression;

import java.util.Map;

/**
 * Evaluates a restricted template expression against a data context.
 *
 * <p>Expressions are intentionally limited (no method calls on user data, no reflection, no
 * assignment) to prevent RCE via untrusted template authors. The grammar lives in {@code
 * TmpExpr.g4}.
 */
public interface ExpressionEvaluator {

  /**
   * Evaluate {@code expression} against the given data context.
   *
   * @param expression source expression, e.g. {@code order.total > 1000 && !client.vip}
   * @param data hierarchical data accessible via dot notation
   * @return result as {@link Object} (Boolean, Number, String, or null)
   */
  Object evaluate(String expression, Map<String, Object> data);

  /** Convenience — throws {@link ExpressionException} if the result is not a Boolean. */
  boolean evaluateBoolean(String expression, Map<String, Object> data);

  /** Parse-only validation — used when publishing template versions. */
  void validate(String expression);
}
