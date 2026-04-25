package cz.komercpoj.tmpmgmt.rendering.application;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import cz.komercpoj.tmpmgmt.expression.AntlrExpressionEvaluator;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.Test;

class DocxRendererTest {

  private final ObjectMapper mapper = new ObjectMapper();
  private final DocxRenderer renderer =
      new DocxRenderer(new AntlrExpressionEvaluator(), new VariableFormatter(), mapper);

  @Test
  void rendersPlainParagraph() throws Exception {
    JsonNode ast =
        mapper.readTree(
            """
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
    JsonNode ast =
        mapper.readTree(
            """
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
    JsonNode ast =
        mapper.readTree(
            """
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
    JsonNode ast =
        mapper.readTree(
            """
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
    JsonNode ast =
        mapper.readTree(
            """
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
    ast.set(
        "content",
        mapper
            .createArrayNode()
            .add(
                mapper
                    .createObjectNode()
                    .put("type", "paragraph")
                    .set(
                        "content",
                        mapper
                            .createArrayNode()
                            .add(mapper.createObjectNode().put("type", "text").put("text", "A:"))
                            .add(
                                mapper
                                    .createObjectNode()
                                    .put("type", "variable")
                                    .set(
                                        "attrs", mapper.createObjectNode().put("path", "nope"))))));

    byte[] docx = renderer.render(ast, Map.of());
    // Didn't throw; just renders empty where the variable would have been.
    assertThat(docx).isNotEmpty();
    assertThat(isZip(docx)).isTrue();
  }

  @Test
  void repeatBlock_iteratesListAndBindsEachVariable() throws Exception {
    JsonNode ast =
        mapper.readTree(
            """
            {
              "type": "doc",
              "content": [
                {
                  "type": "repeatBlock",
                  "attrs": { "each": "item", "in": "order.items" },
                  "content": [
                    { "type": "paragraph", "content": [
                      { "type": "variable", "attrs": { "path": "item.name" } },
                      { "type": "text", "text": " — " },
                      { "type": "variable", "attrs": { "path": "item.qty" } }
                    ]}
                  ]
                }
              ]
            }
            """);

    Map<String, Object> data =
        Map.of(
            "order",
            Map.of(
                "items",
                List.of(
                    Map.of("name", "Widget", "qty", 3),
                    Map.of("name", "Gadget", "qty", 1),
                    Map.of("name", "Thingamajig", "qty", 5))));

    byte[] docx = renderer.render(ast, data);
    String xml = extractDocumentXml(docx);
    // Each token lives in its own <w:r> run — assert them individually.
    assertThat(xml).contains("Widget").contains("Gadget").contains("Thingamajig");
    assertThat(xml).contains(">3<").contains(">1<").contains(">5<");
  }

  @Test
  void repeatBlock_emptyList_rendersNothing() throws Exception {
    JsonNode ast =
        mapper.readTree(
            """
            {
              "type": "doc",
              "content": [
                { "type": "paragraph", "content": [{ "type": "text", "text": "Header." }] },
                {
                  "type": "repeatBlock",
                  "attrs": { "each": "item", "in": "order.items" },
                  "content": [
                    { "type": "paragraph", "content": [
                      { "type": "variable", "attrs": { "path": "item.name" } }
                    ]}
                  ]
                },
                { "type": "paragraph", "content": [{ "type": "text", "text": "Footer." }] }
              ]
            }
            """);

    byte[] docx = renderer.render(ast, Map.of("order", Map.of("items", List.of())));
    String xml = extractDocumentXml(docx);
    assertThat(xml).contains("Header.").contains("Footer.");
    // No iteration output between them.
  }

  @Test
  void repeatBlock_missingPath_rendersNothing() throws Exception {
    JsonNode ast =
        mapper.readTree(
            """
            {
              "type": "doc",
              "content": [
                {
                  "type": "repeatBlock",
                  "attrs": { "each": "item", "in": "totally.missing" },
                  "content": [
                    { "type": "paragraph", "content": [{ "type": "text", "text": "nope" }] }
                  ]
                }
              ]
            }
            """);

    byte[] docx = renderer.render(ast, Map.of());
    String xml = extractDocumentXml(docx);
    assertThat(xml).doesNotContain("nope");
  }

  @Test
  void repeatBlock_withConditionInsideUsingEachVariable() throws Exception {
    JsonNode ast =
        mapper.readTree(
            """
            {
              "type": "doc",
              "content": [
                {
                  "type": "repeatBlock",
                  "attrs": { "each": "item", "in": "items" },
                  "content": [
                    {
                      "type": "conditionBlock",
                      "attrs": { "when": "item.price > 100" },
                      "content": [
                        { "type": "paragraph", "content": [
                          { "type": "text", "text": "Drahý: " },
                          { "type": "variable", "attrs": { "path": "item.name" } }
                        ]}
                      ]
                    }
                  ]
                }
              ]
            }
            """);

    Map<String, Object> data =
        Map.of(
            "items",
            List.of(
                Map.of("name", "Levný", "price", 50),
                Map.of("name", "Drahý", "price", 200),
                Map.of("name", "Střední", "price", 90)));

    byte[] docx = renderer.render(ast, data);
    String xml = extractDocumentXml(docx);
    assertThat(xml).contains("Drahý");
    assertThat(xml).doesNotContain("Levný");
    assertThat(xml).doesNotContain("Střední");
  }

  @Test
  void variableWithCurrencyFormat_rendersLocalizedValue() throws Exception {
    JsonNode ast =
        mapper.readTree(
            """
            {
              "type": "doc",
              "content": [
                { "type": "paragraph", "content": [
                  { "type": "text", "text": "Cena: " },
                  { "type": "variable",
                    "attrs": { "path": "order.total", "format": "currency:CZK" } }
                ]}
              ]
            }
            """);

    byte[] docx = renderer.render(ast, Map.of("order", Map.of("total", 12500)));
    String xml = extractDocumentXml(docx);
    // Czech locale: non-breaking space as grouping → "12 500,00 Kč" with NBSP
    assertThat(xml).contains("Cena:");
    assertThat(xml).contains("500");
    assertThat(xml).contains(",00");
    assertThat(xml).contains("Kč");
  }

  @Test
  void variableWithDateFormat_rendersLocalizedDate() throws Exception {
    JsonNode ast =
        mapper.readTree(
            """
            {
              "type": "doc",
              "content": [
                { "type": "paragraph", "content": [
                  { "type": "text", "text": "Datum: " },
                  { "type": "variable",
                    "attrs": { "path": "order.date", "format": "date:dd.MM.yyyy" } }
                ]}
              ]
            }
            """);

    byte[] docx = renderer.render(ast, Map.of("order", Map.of("date", "2026-04-23")));
    String xml = extractDocumentXml(docx);
    assertThat(xml).contains("23.04.2026");
  }

  @Test
  void nestedRepeatBlocks_shadowingScope() throws Exception {
    JsonNode ast =
        mapper.readTree(
            """
            {
              "type": "doc",
              "content": [
                {
                  "type": "repeatBlock",
                  "attrs": { "each": "outer", "in": "groups" },
                  "content": [
                    {
                      "type": "repeatBlock",
                      "attrs": { "each": "inner", "in": "outer.items" },
                      "content": [
                        { "type": "paragraph", "content": [
                          { "type": "variable", "attrs": { "path": "outer.label" } },
                          { "type": "text", "text": ":" },
                          { "type": "variable", "attrs": { "path": "inner" } }
                        ]}
                      ]
                    }
                  ]
                }
              ]
            }
            """);

    Map<String, Object> data =
        Map.of(
            "groups",
            List.of(
                Map.of("label", "A", "items", List.of("a1", "a2")),
                Map.of("label", "B", "items", List.of("b1"))));

    byte[] docx = renderer.render(ast, data);
    String xml = extractDocumentXml(docx);
    // Both outer labels appear as text runs, along with all inner values — proves that the
    // inner loop saw `outer.label` from the enclosing scope and that `inner` shadowed nothing
    // missing.
    assertThat(xml).contains(">A</w:t>").contains(">B</w:t>");
    assertThat(xml).contains(">a1</w:t>").contains(">a2</w:t>").contains(">b1</w:t>");
  }

  private static boolean isZip(byte[] bytes) {
    return bytes.length > 4 && bytes[0] == 'P' && bytes[1] == 'K' && bytes[2] == 3 && bytes[3] == 4;
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
