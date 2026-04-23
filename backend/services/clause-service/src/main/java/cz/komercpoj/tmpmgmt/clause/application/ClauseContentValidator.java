package cz.komercpoj.tmpmgmt.clause.application;

import com.fasterxml.jackson.databind.JsonNode;
import cz.komercpoj.tmpmgmt.common.ValidationException;
import cz.komercpoj.tmpmgmt.expression.ExpressionEvaluator;
import cz.komercpoj.tmpmgmt.expression.ExpressionException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Validates a clause AST fragment. Clauses are embeddable — root is a {@code fragment} node with
 * block children (paragraph, conditionBlock, repeatBlock, …), not a full {@code doc}. Expressions
 * are validated via the shared {@link ExpressionEvaluator}.
 */
@Component
public class ClauseContentValidator {

    private final ExpressionEvaluator expressions;

    public ClauseContentValidator(ExpressionEvaluator expressions) {
        this.expressions = expressions;
    }

    public void validate(JsonNode content) {
        List<String> violations = new ArrayList<>();
        validateRoot(content, violations);
        if (!violations.isEmpty()) {
            throw new ValidationException(
                    "clause.content_invalid", "Clause content is invalid.", violations);
        }
    }

    private void validateRoot(JsonNode content, List<String> violations) {
        if (content == null || !content.isObject()) {
            violations.add("content: must be a JSON object.");
            return;
        }
        String type = content.path("type").asText(null);
        if (!"fragment".equals(type)) {
            violations.add("content.type: expected 'fragment', got '" + type + "'.");
        }
        JsonNode children = content.path("content");
        if (!children.isArray()) {
            violations.add("content.content: expected an array.");
            return;
        }
        walk(children, violations);
    }

    private void walk(JsonNode nodes, List<String> violations) {
        Iterator<JsonNode> it = nodes.elements();
        while (it.hasNext()) {
            JsonNode node = it.next();
            String type = node.path("type").asText("");
            switch (type) {
                case "conditionBlock" -> validateExpr(node, "attrs.when", violations);
                case "repeatBlock" -> validateExpr(node, "attrs.in", violations);
                default -> { /* other node types require no expression check */ }
            }
            JsonNode child = node.path("content");
            if (child.isArray()) {
                walk(child, violations);
            }
        }
    }

    private void validateExpr(JsonNode node, String path, List<String> violations) {
        String[] segments = path.split("\\.");
        JsonNode cursor = node;
        for (String seg : segments) {
            cursor = cursor.path(seg);
        }
        String expr = cursor.asText(null);
        if (expr == null) {
            violations.add(path + ": missing expression.");
            return;
        }
        try {
            expressions.validate(expr);
        } catch (ExpressionException ex) {
            violations.add(path + ": " + ex.getMessage());
        }
    }
}
