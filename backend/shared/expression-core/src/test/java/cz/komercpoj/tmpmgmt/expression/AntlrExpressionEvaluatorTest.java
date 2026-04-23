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
        Map<String, Object> data = Map.of(
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
        Map<String, Object> data = Map.of(
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
        assertThatThrownBy(() -> eval.validate(""))
                .isInstanceOf(ExpressionException.class);
    }

    @Test
    void unknownFunction_throws() {
        assertThatThrownBy(() -> eval.evaluate("eval('exit()')", Map.of()))
                .isInstanceOf(ExpressionException.class)
                .hasMessageContaining("Unknown function");
    }

    @Test
    void realWorldConditionBlock() {
        Map<String, Object> data = Map.of(
                "order", Map.of("total", 150000),
                "client", Map.of("vip", true));
        assertThat(eval.evaluateBoolean("order.total > 100000 && client.vip", data)).isTrue();
        assertThat(eval.evaluateBoolean("order.total > 100000 && !client.vip", data)).isFalse();
    }
}
