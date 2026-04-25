package cz.komercpoj.tmpmgmt.template.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import cz.komercpoj.tmpmgmt.common.ValidationException;
import cz.komercpoj.tmpmgmt.expression.ExpressionEvaluator;
import cz.komercpoj.tmpmgmt.expression.ExpressionException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Structural + expression validation for a template AST and its variables schema.
 *
 * <ul>
 *   <li>variablesSchema must parse as a JSON Schema (Draft 2020-12).
 *   <li>content must be a {@code doc} node with an array of children.
 *   <li>every {@code conditionBlock.when} / {@code repeatBlock.in} expression must be parseable by
 *       the restricted expression language.
 * </ul>
 */
@Component
public class TemplateContentValidator {

  private final JsonSchemaFactory schemaFactory;
  private final ExpressionEvaluator expressions;

  public TemplateContentValidator(ExpressionEvaluator expressions) {
    this.schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    this.expressions = expressions;
  }

  public void validate(JsonNode content, JsonNode variablesSchema) {
    List<String> violations = new ArrayList<>();
    validateSchema(variablesSchema, violations);
    validateContent(content, violations);
    if (!violations.isEmpty()) {
      throw new ValidationException(
          "template.content_invalid", "Template content is invalid.", violations);
    }
  }

  private void validateSchema(JsonNode variablesSchema, List<String> violations) {
    if (variablesSchema == null || !variablesSchema.isObject()) {
      violations.add("variablesSchema: must be a JSON object.");
      return;
    }
    try {
      JsonSchema parsed = schemaFactory.getSchema(variablesSchema);
      // Touch the schema to force resolution; networknt parses lazily otherwise.
      parsed.initializeValidators();
    } catch (RuntimeException ex) {
      violations.add("variablesSchema: not a valid JSON Schema — " + ex.getMessage());
    }
  }

  private void validateContent(JsonNode content, List<String> violations) {
    if (content == null || !content.isObject()) {
      violations.add("content: must be a JSON object.");
      return;
    }
    String type = content.path("type").asText(null);
    if (!"doc".equals(type)) {
      violations.add("content.type: expected 'doc', got '" + type + "'.");
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
        default -> {
          /* other node types require no expression check */
        }
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
