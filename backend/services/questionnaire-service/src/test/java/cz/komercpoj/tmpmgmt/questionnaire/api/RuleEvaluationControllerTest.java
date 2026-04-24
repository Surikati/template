package cz.komercpoj.tmpmgmt.questionnaire.api;

import static org.assertj.core.api.Assertions.assertThat;

import cz.komercpoj.tmpmgmt.expression.AntlrExpressionEvaluator;
import cz.komercpoj.tmpmgmt.questionnaire.api.dto.EvaluateRulesRequest;
import cz.komercpoj.tmpmgmt.questionnaire.api.dto.EvaluateRulesRequest.RuleInput;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RuleEvaluationControllerTest {

    private final RuleEvaluationController controller =
            new RuleEvaluationController(new AntlrExpressionEvaluator());

    @Test
    void evaluatesMixedRulesAgainstSharedContext() {
        var req = new EvaluateRulesRequest(
                List.of(
                        new RuleInput("section-A", "client.type == 'B2B'"),
                        new RuleInput("question-1", "amount > 100"),
                        new RuleInput("question-2", "client.type == 'B2C'")),
                Map.of("client", Map.of("type", "B2B"), "amount", 250));

        var resp = controller.evaluate(req);

        assertThat(resp.results()).hasSize(3);
        assertThat(resp.results().get("section-A").value()).isTrue();
        assertThat(resp.results().get("question-1").value()).isTrue();
        assertThat(resp.results().get("question-2").value()).isFalse();
        assertThat(resp.results().get("section-A").error()).isNull();
    }

    @Test
    void perRuleErrorIsCaptured_othersStillEvaluate() {
        var req = new EvaluateRulesRequest(
                List.of(
                        new RuleInput("good", "x == 5"),
                        new RuleInput("bad-syntax", "x ===== 5")),
                Map.of("x", 5));

        var resp = controller.evaluate(req);

        assertThat(resp.results().get("good").value()).isTrue();
        assertThat(resp.results().get("good").error()).isNull();
        assertThat(resp.results().get("bad-syntax").value()).isFalse();
        assertThat(resp.results().get("bad-syntax").error()).isNotBlank();
    }

    @Test
    void missingPathTreatedAsFalsy_noError() {
        var req = new EvaluateRulesRequest(
                List.of(new RuleInput("k", "missing.field == 'x'")),
                Map.of());

        var resp = controller.evaluate(req);

        // null == 'x' → false, no exception
        assertThat(resp.results().get("k").value()).isFalse();
        assertThat(resp.results().get("k").error()).isNull();
    }

    @Test
    void emptyRules_returnsEmptyMap() {
        var resp = controller.evaluate(new EvaluateRulesRequest(List.of(), Map.of()));
        assertThat(resp.results()).isEmpty();
    }
}
