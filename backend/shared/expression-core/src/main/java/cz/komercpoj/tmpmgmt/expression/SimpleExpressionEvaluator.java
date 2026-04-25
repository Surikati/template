package cz.komercpoj.tmpmgmt.expression;

import java.util.Map;

/**
 * Placeholder implementation. Rejects obviously empty input and provides an evaluate() that always
 * throws — enough to keep services compiling and to fail loudly in environments where nobody wired
 * a real evaluator. Replace with the ANTLR-based evaluator generated from {@code TmpExpr.g4}.
 */
public class SimpleExpressionEvaluator implements ExpressionEvaluator {

  @Override
  public Object evaluate(String expression, Map<String, Object> data) {
    throw new ExpressionException(
        "expression.not_implemented",
        "SimpleExpressionEvaluator does not evaluate. Wire the ANTLR-based evaluator.");
  }

  @Override
  public boolean evaluateBoolean(String expression, Map<String, Object> data) {
    Object v = evaluate(expression, data);
    if (v instanceof Boolean b) return b;
    throw new ExpressionException(
        "expression.not_boolean", "Expression did not evaluate to a boolean: " + expression);
  }

  @Override
  public void validate(String expression) {
    if (expression == null || expression.isBlank()) {
      throw new ExpressionException("expression.empty", "Expression must not be empty.");
    }
  }
}
