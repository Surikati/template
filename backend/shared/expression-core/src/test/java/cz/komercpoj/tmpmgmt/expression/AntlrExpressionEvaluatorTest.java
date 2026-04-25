package cz.komercpoj.tmpmgmt.expression;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AntlrExpressionEvaluatorTest {

  private final ExpressionEvaluator eval = new AntlrExpressionEvaluator();

  @Test
  void literals() {
    assertThat(eval.evaluate("42", Map.of())).isEqualTo(42L);
    assertThat(eval.evaluate("3.14", Map.of())).isEqualTo(3.14);
    assertThat(eval.evaluate("'hello'", Map.of())).isEqualTo("hello");
    assertThat(eval.evaluate("\"hi\"", Map.of())).isEqualTo("hi");
    assertThat(eval.evaluate("true", Map.of())).isEqualTo(true);
    assertThat(eval.evaluate("null", Map.of())).isNull();
  }

  @Test
  void pathAccess_nested() {
    Map<String, Object> data =
        Map.of(
            "client", Map.of("name", "ACME", "revenue", 5000),
            "order", Map.of("items", List.of("a", "b", "c")));
    assertThat(eval.evaluate("client.name", data)).isEqualTo("ACME");
    assertThat(eval.evaluate("client.revenue", data)).isEqualTo(5000);
    assertThat(eval.evaluate("order.items", data)).isInstanceOf(List.class);
    assertThat(eval.evaluate("missing.path", data)).isNull();
  }

  @Test
  void comparisons() {
    Map<String, Object> data = Map.of("total", 1500, "name", "ACME");
    assertThat(eval.evaluateBoolean("total > 1000", data)).isTrue();
    assertThat(eval.evaluateBoolean("total == 1500", data)).isTrue();
    assertThat(eval.evaluateBoolean("total >= 1500", data)).isTrue();
    assertThat(eval.evaluateBoolean("total < 100", data)).isFalse();
    assertThat(eval.evaluateBoolean("name == 'ACME'", data)).isTrue();
    assertThat(eval.evaluateBoolean("name != 'OTHER'", data)).isTrue();
  }

  @Test
  void booleanOps_shortCircuit() {
    assertThat(eval.evaluateBoolean("true && false", Map.of())).isFalse();
    assertThat(eval.evaluateBoolean("true || false", Map.of())).isTrue();
    assertThat(eval.evaluateBoolean("!false", Map.of())).isTrue();
    assertThat(eval.evaluateBoolean("!(1 == 2)", Map.of())).isTrue();

    // Path missing would be null; && should short-circuit.
    Map<String, Object> data = Map.of("a", false);
    assertThat(eval.evaluateBoolean("a && missing.thing > 0", data)).isFalse();
  }

  @Test
  void arithmetic() {
    assertThat(eval.evaluate("1 + 2 * 3", Map.of())).isEqualTo(7L);
    assertThat(eval.evaluate("(1 + 2) * 3", Map.of())).isEqualTo(9L);
    assertThat(eval.evaluate("10 / 4", Map.of())).isEqualTo(2.5);
    assertThat(eval.evaluate("-5 + 3", Map.of())).isEqualTo(-2.0);
  }

  @Test
  void stringConcat() {
    Map<String, Object> data = Map.of("first", "Jan", "last", "Novák");
    assertThat(eval.evaluate("first + ' ' + last", data)).isEqualTo("Jan Novák");
  }

  @Test
  void functions() {
    Map<String, Object> data =
        Map.of(
            "items", List.of(1, 2, 3),
            "name", "ACME",
            "nothing", List.of());
    assertThat(eval.evaluate("len(items)", data)).isEqualTo(3L);
    assertThat(eval.evaluate("len(name)", data)).isEqualTo(4L);
    assertThat(eval.evaluate("empty(nothing)", data)).isEqualTo(true);
    assertThat(eval.evaluate("empty(items)", data)).isEqualTo(false);
    assertThat(eval.evaluate("formatDate('2026-04-23', 'dd.MM.yyyy')", data))
        .isEqualTo("23.04.2026");
  }

  @Test
  void syntaxError_throws() {
    assertThatThrownBy(() -> eval.validate("1 + + 2"))
        .isInstanceOf(ExpressionException.class)
        .hasMessageContaining("Syntax error");
  }

  @Test
  void emptyExpression_fails() {
    assertThatThrownBy(() -> eval.validate("")).isInstanceOf(ExpressionException.class);
  }

  @Test
  void unknownFunction_throws() {
    assertThatThrownBy(() -> eval.evaluate("eval('exit()')", Map.of()))
        .isInstanceOf(ExpressionException.class)
        .hasMessageContaining("Unknown function");
  }

  @Test
  void realWorldConditionBlock() {
    Map<String, Object> data =
        Map.of(
            "order", Map.of("total", 150000),
            "client", Map.of("vip", true));
    assertThat(eval.evaluateBoolean("order.total > 100000 && client.vip", data)).isTrue();
    assertThat(eval.evaluateBoolean("order.total > 100000 && !client.vip", data)).isFalse();
  }

  @Test
  void inOperator_withListLiteral() {
    Map<String, Object> data = Map.of("country", "CZ");
    assertThat(eval.evaluateBoolean("country in ['CZ', 'SK']", data)).isTrue();
    assertThat(eval.evaluateBoolean("country in ['DE', 'AT']", data)).isFalse();
    assertThat(eval.evaluateBoolean("'PL' in ['CZ', 'SK', 'PL']", Map.of())).isTrue();
  }

  @Test
  void inOperator_withListFromData_andNumericEqualitySemantics() {
    Map<String, Object> data =
        Map.of(
            "tags", List.of("vip", "enterprise"),
            "scores", List.of(1, 2, 3));
    assertThat(eval.evaluateBoolean("'vip' in tags", data)).isTrue();
    assertThat(eval.evaluateBoolean("'unknown' in tags", data)).isFalse();
    // long literal vs Integer in collection — eq() coerces both to double for numbers
    assertThat(eval.evaluateBoolean("2 in scores", data)).isTrue();
  }

  @Test
  void inOperator_emptyAndNullCollection() {
    assertThat(eval.evaluateBoolean("'x' in []", Map.of())).isFalse();
    assertThat(eval.evaluateBoolean("'x' in missing.list", Map.of())).isFalse();
  }

  @Test
  void inOperator_nonCollectionRhs_throws() {
    assertThatThrownBy(() -> eval.evaluate("'x' in 5", Map.of()))
        .isInstanceOf(ExpressionException.class)
        .hasMessageContaining("must be a list");
  }

  @Test
  void stringFunctions() {
    Map<String, Object> data = Map.of("email", "Alice@Example.com");
    assertThat(eval.evaluate("contains('hello world', 'world')", Map.of())).isEqualTo(true);
    assertThat(eval.evaluate("contains('abc', 'xyz')", Map.of())).isEqualTo(false);
    assertThat(eval.evaluate("startsWith('cz_company', 'cz_')", Map.of())).isEqualTo(true);
    assertThat(eval.evaluate("endsWith(email, '.com')", data)).isEqualTo(true);
    assertThat(eval.evaluate("lower(email)", data)).isEqualTo("alice@example.com");
    assertThat(eval.evaluate("upper('hi')", Map.of())).isEqualTo("HI");
  }

  @Test
  void dateArithmetic() {
    assertThat(eval.evaluate("addDays('2026-04-23', 7)", Map.of())).isEqualTo("2026-04-30");
    assertThat(eval.evaluate("addDays('2026-04-23', -23)", Map.of())).isEqualTo("2026-03-31");
    assertThat(eval.evaluate("daysBetween('2026-04-01', '2026-04-30')", Map.of())).isEqualTo(29L);
    // Direction-agnostic
    assertThat(eval.evaluate("daysBetween('2026-04-30', '2026-04-01')", Map.of())).isEqualTo(29L);
  }

  @Test
  void datesCompareLexicographically_iso8601IsSortable() {
    // ISO format means string comparison happens to work as date comparison.
    Map<String, Object> data = Map.of("expiry", "2026-12-31");
    assertThat(eval.evaluateBoolean("expiry > '2026-01-01'", data)).isTrue();
    assertThat(eval.evaluateBoolean("expiry < today() && false", data)).isFalse();
  }

  @Test
  void identifierContainingIn_lexedAsIdent_notAsKeyword() {
    // `index`, `instance`, `infinite` should NOT be lexed as the IN keyword.
    Map<String, Object> data = Map.of("index", 5, "instance", Map.of("id", "abc"));
    assertThat(eval.evaluate("index", data)).isEqualTo(5);
    assertThat(eval.evaluate("instance.id", data)).isEqualTo("abc");
  }
}
