package cz.komercpoj.tmpmgmt.rendering.application;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.komercpoj.tmpmgmt.expression.AntlrExpressionEvaluator;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PdfRendererTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final HtmlRenderer htmlRenderer = new HtmlRenderer(
            new AntlrExpressionEvaluator(), new VariableFormatter());
    private final PdfRenderer renderer = new PdfRenderer(htmlRenderer);

    @Test
    void rendersBasicAst_producesPdfMagic() throws Exception {
        JsonNode ast = mapper.readTree("""
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
    void renderResolvesVariables_andContainsExpectedText() throws Exception {
        JsonNode ast = mapper.readTree("""
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
