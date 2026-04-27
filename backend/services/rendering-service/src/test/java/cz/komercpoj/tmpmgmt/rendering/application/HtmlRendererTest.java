package cz.komercpoj.tmpmgmt.rendering.application;

import static org.assertj.core.api.Assertions.*;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import cz.komercpoj.tmpmgmt.expression.AntlrExpressionEvaluator;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class HtmlRendererTest {

  private final ObjectMapper mapper = new ObjectMapper();
  private final HtmlRenderer renderer =
      new HtmlRenderer(new AntlrExpressionEvaluator(), new VariableFormatter());

  @Test
  void rendersParagraphAndText() throws Exception {
    JsonNode ast =
        mapper.readTree(
            """
            { "type": "doc", "content": [
              { "type": "paragraph", "content": [
                { "type": "text", "text": "Ahoj světe" }
              ]}
            ]}
            """);
    String html = renderer.render(ast, Map.of());
    assertThat(html).contains("<p>").contains("Ahoj světe").contains("</p>");
  }

  @Test
  void headingUsesHLevelTag() throws Exception {
    JsonNode ast =
        mapper.readTree(
            """
            { "type": "doc", "content": [
              { "type": "heading", "attrs": { "level": 2 }, "content": [
                { "type": "text", "text": "Smlouva" }
              ]}
            ]}
            """);
    String html = renderer.render(ast, Map.of());
    assertThat(html).contains("<h2>").contains("Smlouva").contains("</h2>");
  }

  @Test
  void variableWithFormat_wrappedInHighlightSpan() throws Exception {
    JsonNode ast =
        mapper.readTree(
            """
            { "type": "doc", "content": [
              { "type": "paragraph", "content": [
                { "type": "text", "text": "Cena: " },
                { "type": "variable",
                  "attrs": { "path": "order.total", "format": "currency:CZK" } }
              ]}
            ]}
            """);
    String html = renderer.render(ast, Map.of("order", Map.of("total", 1000)));
    assertThat(html).contains("Cena:").contains("<span style=").contains("Kč");
  }

  @Test
  void textContentIsHtmlEscaped_preventingXss() throws Exception {
    JsonNode ast =
        mapper.readTree(
            """
            { "type": "doc", "content": [
              { "type": "paragraph", "content": [
                { "type": "text", "text": "<script>alert('xss')</script>" },
                { "type": "variable", "attrs": { "path": "evil" } }
              ]}
            ]}
            """);
    String html = renderer.render(ast, Map.of("evil", "<img src=x onerror=alert(1)>"));
    assertThat(html).doesNotContain("<script>");
    assertThat(html).doesNotContain("<img src=x");
    assertThat(html).contains("&lt;script&gt;").contains("&lt;img src=x");
  }

  @Test
  void conditionBlock_skippedWhenFalse() throws Exception {
    JsonNode ast =
        mapper.readTree(
            """
            { "type": "doc", "content": [
              { "type": "paragraph", "content": [{ "type": "text", "text": "Base." }] },
              { "type": "conditionBlock", "attrs": { "when": "total > 100" }, "content": [
                { "type": "paragraph", "content": [{ "type": "text", "text": "Extended." }] }
              ]}
            ]}
            """);
    String html = renderer.render(ast, Map.of("total", 50));
    assertThat(html).contains("Base.");
    assertThat(html).doesNotContain("Extended.");
  }

  @Test
  void repeatBlock_iteratesWithScopedVariables() throws Exception {
    JsonNode ast =
        mapper.readTree(
            """
            { "type": "doc", "content": [
              { "type": "repeatBlock", "attrs": { "each": "item", "in": "items" }, "content": [
                { "type": "paragraph", "content": [
                  { "type": "variable", "attrs": { "path": "item.name" } }
                ]}
              ]}
            ]}
            """);
    String html =
        renderer.render(
            ast, Map.of("items", List.of(Map.of("name", "Widget"), Map.of("name", "Gadget"))));
    assertThat(html).contains("Widget").contains("Gadget");
    // Two paragraphs, not one
    assertThat(html.split("<p>")).hasSizeGreaterThanOrEqualTo(3);
  }
}
