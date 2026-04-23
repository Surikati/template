package cz.komercpoj.tmpmgmt.rendering.application;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import cz.komercpoj.tmpmgmt.expression.AntlrExpressionEvaluator;
import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.Test;

class DocxRendererTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final DocxRenderer renderer = new DocxRenderer(new AntlrExpressionEvaluator(), mapper);

    @Test
    void rendersPlainParagraph() throws Exception {
        JsonNode ast = mapper.readTree("""
            {
              "type": "doc",
              "content": [
                { "type": "paragraph", "content": [
                  { "type": "text", "text": "Hello, world!" }
                ]}
              ]
            }
            """);

        byte[] docx = renderer.render(ast, Map.of());
        assertThat(docx).isNotEmpty();
        assertThat(isZip(docx)).isTrue();
        assertThat(extractDocumentXml(docx)).contains("Hello, world!");
    }

    @Test
    void substitutesVariableFromData() throws Exception {
        JsonNode ast = mapper.readTree("""
            {
              "type": "doc",
              "content": [
                { "type": "paragraph", "content": [
                  { "type": "text", "text": "Klient: " },
                  { "type": "variable", "attrs": { "path": "client.name", "dataType": "STRING" } }
                ]}
              ]
            }
            """);

        byte[] docx = renderer.render(ast, Map.of("client", Map.of("name", "ACME s.r.o.")));
        String xml = extractDocumentXml(docx);
        assertThat(xml).contains("Klient:").contains("ACME s.r.o.");
    }

    @Test
    void conditionBlock_includedWhenTrue() throws Exception {
        JsonNode ast = mapper.readTree("""
            {
              "type": "doc",
              "content": [
                { "type": "paragraph", "content": [
                  { "type": "text", "text": "Základní text." }
                ]},
                { "type": "conditionBlock", "attrs": { "when": "order.total > 100000" }, "content": [
                  { "type": "paragraph", "content": [
                    { "type": "text", "text": "Platí rozšířené podmínky." }
                  ]}
                ]}
              ]
            }
            """);

        byte[] docx = renderer.render(ast, Map.of("order", Map.of("total", 150000)));
        String xml = extractDocumentXml(docx);
        assertThat(xml).contains("Základní text.").contains("Platí rozšířené podmínky.");
    }

    @Test
    void conditionBlock_excludedWhenFalse() throws Exception {
        JsonNode ast = mapper.readTree("""
            {
              "type": "doc",
              "content": [
                { "type": "paragraph", "content": [
                  { "type": "text", "text": "Základní text." }
                ]},
                { "type": "conditionBlock", "attrs": { "when": "order.total > 100000" }, "content": [
                  { "type": "paragraph", "content": [
                    { "type": "text", "text": "Rozšířené podmínky." }
                  ]}
                ]}
              ]
            }
            """);

        byte[] docx = renderer.render(ast, Map.of("order", Map.of("total", 5000)));
        String xml = extractDocumentXml(docx);
        assertThat(xml).contains("Základní text.");
        assertThat(xml).doesNotContain("Rozšířené podmínky.");
    }

    @Test
    void heading_usesHeadingStyle() throws Exception {
        JsonNode ast = mapper.readTree("""
            {
              "type": "doc",
              "content": [
                { "type": "heading", "attrs": { "level": 1 }, "content": [
                  { "type": "text", "text": "Smlouva" }
                ]}
              ]
            }
            """);

        byte[] docx = renderer.render(ast, Map.of());
        String xml = extractDocumentXml(docx);
        assertThat(xml).contains("Smlouva");
        assertThat(xml).contains("Heading1");
    }

    @Test
    void variableResolvingToMissing_yieldsEmptyString() throws Exception {
        ObjectNode ast = mapper.createObjectNode().put("type", "doc");
        ast.set("content", mapper.createArrayNode().add(
                mapper.createObjectNode().put("type", "paragraph").set(
                        "content",
                        mapper.createArrayNode().add(
                                mapper.createObjectNode().put("type", "text").put("text", "A:"))
                                .add(mapper.createObjectNode()
                                        .put("type", "variable")
                                        .set("attrs", mapper.createObjectNode().put("path", "nope"))))));

        byte[] docx = renderer.render(ast, Map.of());
        // Didn't throw; just renders empty where the variable would have been.
        assertThat(docx).isNotEmpty();
        assertThat(isZip(docx)).isTrue();
    }

    private static boolean isZip(byte[] bytes) {
        return bytes.length > 4
                && bytes[0] == 'P' && bytes[1] == 'K' && bytes[2] == 3 && bytes[3] == 4;
    }

    private static String extractDocumentXml(byte[] docx) throws Exception {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(docx))) {
            ZipEntry e;
            while ((e = zis.getNextEntry()) != null) {
                if (e.getName().equals("word/document.xml")) {
                    return new String(zis.readAllBytes());
                }
            }
        }
        throw new IllegalStateException("word/document.xml not found in DOCX");
    }
}
