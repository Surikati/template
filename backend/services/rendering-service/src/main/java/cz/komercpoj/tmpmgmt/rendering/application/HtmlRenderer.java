package cz.komercpoj.tmpmgmt.rendering.application;

import com.fasterxml.jackson.databind.JsonNode;
import cz.komercpoj.tmpmgmt.expression.ExpressionEvaluator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * HTML preview variant of {@link DocxRenderer}. Walks the same AST and emits sanitized, styled HTML
 * for on-screen preview. Values from text/variable nodes are HTML-escaped. No script/style tags are
 * ever emitted; the FE can drop the output into a sanitized div.
 */
@Component
public class HtmlRenderer {

  private static final String PREFIX =
      "<div class=\"tmpmgmt-preview\" style=\""
          + "font-family: Georgia, 'Times New Roman', serif;"
          + "line-height: 1.6;"
          + "color: #1f2937;"
          + "max-width: 720px;"
          + "margin: 0 auto;"
          + "padding: 2rem;"
          + "background: #ffffff;"
          + "\">";

  private static final String SUFFIX = "</div>";

  private final ExpressionEvaluator expressions;
  private final VariableFormatter formatter;

  public HtmlRenderer(ExpressionEvaluator expressions, VariableFormatter formatter) {
    this.expressions = expressions;
    this.formatter = formatter;
  }

  public String render(JsonNode ast, Map<String, Object> data) {
    StringBuilder sb = new StringBuilder(PREFIX);
    JsonNode children = ast.path("content");
    if (children.isArray()) {
      walkBlocks(children, data, sb);
    }
    sb.append(SUFFIX);
    return sb.toString();
  }

  private void walkBlocks(JsonNode blocks, Map<String, Object> data, StringBuilder sb) {
    Iterator<JsonNode> it = blocks.elements();
    while (it.hasNext()) {
      JsonNode node = it.next();
      String type = node.path("type").asText("");
      switch (type) {
        case "paragraph" -> emitParagraph(node, data, sb, "p", "");
        case "heading" -> {
          int level = Math.max(1, Math.min(6, node.path("attrs").path("level").asInt(1)));
          emitParagraph(node, data, sb, "h" + level, "");
        }
        case "conditionBlock" -> {
          String when = node.path("attrs").path("when").asText("");
          if (!when.isBlank() && expressions.evaluateBoolean(when, data)) {
            JsonNode inner = node.path("content");
            if (inner.isArray()) walkBlocks(inner, data, sb);
          }
        }
        case "repeatBlock" -> emitRepeat(node, data, sb);
        default -> {
          /* unknown block types silently skipped */
        }
      }
    }
  }

  private void emitParagraph(
      JsonNode paragraphNode,
      Map<String, Object> data,
      StringBuilder sb,
      String tag,
      String extraStyle) {
    sb.append("<").append(tag);
    if (!extraStyle.isEmpty()) sb.append(" style=\"").append(extraStyle).append("\"");
    sb.append(">");

    JsonNode inlines = paragraphNode.path("content");
    if (inlines.isArray()) {
      Iterator<JsonNode> it = inlines.elements();
      while (it.hasNext()) {
        JsonNode inline = it.next();
        switch (inline.path("type").asText("")) {
          case "text" -> sb.append(escapeHtml(inline.path("text").asText("")));
          case "variable" -> {
            JsonNode attrs = inline.path("attrs");
            String path = attrs.path("path").asText("");
            String format = attrs.path("format").asText(null);
            Object resolved = resolvePath(path, data);
            String formatted = formatter.format(resolved, format);
            // Highlight variable substitutions so previewers see what was filled.
            sb.append("<span style=\"background:#fef3c7;padding:0 2px;border-radius:2px;\">")
                .append(escapeHtml(formatted))
                .append("</span>");
          }
          default -> {
            /* skip */
          }
        }
      }
    }
    sb.append("</").append(tag).append(">");
  }

  private void emitRepeat(JsonNode node, Map<String, Object> data, StringBuilder sb) {
    String each = node.path("attrs").path("each").asText("");
    String inExpr = node.path("attrs").path("in").asText("");
    if (each.isBlank() || inExpr.isBlank()) return;

    Object value = expressions.evaluate(inExpr, data);
    if (!(value instanceof List<?> list)) return;
    JsonNode inner = node.path("content");
    if (!inner.isArray()) return;

    for (Object item : list) {
      Map<String, Object> scoped = new HashMap<>(data);
      scoped.put(each, item);
      walkBlocks(inner, scoped, sb);
    }
  }

  @SuppressWarnings("unchecked")
  private Object resolvePath(String path, Map<String, Object> data) {
    if (path == null || path.isBlank()) return null;
    Object cursor = data;
    for (String seg : path.split("\\.")) {
      if (!(cursor instanceof Map<?, ?> m)) return null;
      cursor = ((Map<String, Object>) m).get(seg);
      if (cursor == null) return null;
    }
    return cursor;
  }

  /** Minimal HTML entity escape — sufficient for text content within paragraph tags. */
  private static String escapeHtml(String value) {
    if (value == null) return "";
    StringBuilder out = new StringBuilder(value.length());
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      switch (c) {
        case '&' -> out.append("&amp;");
        case '<' -> out.append("&lt;");
        case '>' -> out.append("&gt;");
        case '"' -> out.append("&quot;");
        case '\'' -> out.append("&#39;");
        default -> out.append(c);
      }
    }
    return out.toString();
  }
}
