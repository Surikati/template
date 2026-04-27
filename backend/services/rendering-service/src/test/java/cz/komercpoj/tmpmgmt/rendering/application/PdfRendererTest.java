package cz.komercpoj.tmpmgmt.rendering.application;

import static org.assertj.core.api.Assertions.*;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import cz.komercpoj.tmpmgmt.expression.AntlrExpressionEvaluator;
import cz.komercpoj.tmpmgmt.rendering.config.RenderingProperties;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PdfRendererTest {

  private final ObjectMapper mapper = new ObjectMapper();
  private final HtmlRenderer htmlRenderer =
      new HtmlRenderer(new AntlrExpressionEvaluator(), new VariableFormatter());
  private final PdfRenderer renderer =
      new PdfRenderer(htmlRenderer, new RenderingProperties(null, null, null, null, null, null));

  @Test
  void rendersBasicAst_producesPdfMagic() throws Exception {
    JsonNode ast =
        mapper.readTree(
            """
            { "type": "doc", "content": [
              { "type": "heading", "attrs": { "level": 1 }, "content": [
                { "type": "text", "text": "Smlouva" }
              ]},
              { "type": "paragraph", "content": [
                { "type": "text", "text": "Mezi smluvními stranami:" }
              ]}
            ]}
            """);

    byte[] pdf = renderer.render(ast, Map.of());

    // Every PDF starts with "%PDF-".
    assertThat(pdf).hasSizeGreaterThan(100);
    assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
  }

  @Test
  void missingFontPath_logsWarningAndFallsBack() throws Exception {
    // Bogus path — should warn but still produce a valid PDF via fallback.
    PdfRenderer fallback =
        new PdfRenderer(
            htmlRenderer,
            new RenderingProperties(null, null, null, "/nonexistent/font.ttf", null, null));
    JsonNode ast =
        mapper.readTree(
            """
            { "type": "doc", "content": [
              { "type": "paragraph", "content": [
                { "type": "text", "text": "Test" }
              ]}
            ]}
            """);

    byte[] pdf = fallback.render(ast, Map.of());

    assertThat(pdf).hasSizeGreaterThan(100);
    assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
  }

  @Test
  void bogusClasspathFont_logsWarningAndFallsBack() throws Exception {
    // Resource doesn't exist on the test classpath — renderer should warn,
    // skip the font, and still produce a valid PDF.
    PdfRenderer fallback =
        new PdfRenderer(
            htmlRenderer,
            new RenderingProperties(null, null, null, null, null, "fonts/nonexistent.ttf"));
    JsonNode ast =
        mapper.readTree(
            """
            { "type": "doc", "content": [
              { "type": "paragraph", "content": [
                { "type": "text", "text": "Test" }
              ]}
            ]}
            """);

    byte[] pdf = fallback.render(ast, Map.of());

    assertThat(pdf).hasSizeGreaterThan(100);
    assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
  }

  @Test
  void renderResolvesVariables_andContainsExpectedText() throws Exception {
    JsonNode ast =
        mapper.readTree(
            """
            { "type": "doc", "content": [
              { "type": "paragraph", "content": [
                { "type": "text", "text": "Klient: " },
                { "type": "variable", "attrs": { "path": "client.name" } }
              ]}
            ]}
            """);

    byte[] pdf = renderer.render(ast, Map.of("client", Map.of("name", "ACME s.r.o.")));

    assertThat(pdf).hasSizeGreaterThan(100);
    // Sanity check — the underlying HTML went through the resolver before PDF generation.
    String html = htmlRenderer.render(ast, Map.of("client", Map.of("name", "ACME s.r.o.")));
    assertThat(html).contains("ACME s.r.o.");
  }
}
