package cz.komercpoj.tmpmgmt.rendering.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.komercpoj.tmpmgmt.common.DomainException;
import cz.komercpoj.tmpmgmt.expression.ExpressionEvaluator;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.docx4j.jaxb.Context;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;
import org.docx4j.wml.ObjectFactory;
import org.docx4j.wml.P;
import org.docx4j.wml.PPr;
import org.docx4j.wml.PPrBase;
import org.docx4j.wml.R;
import org.docx4j.wml.Text;
import org.springframework.stereotype.Component;

/**
 * Walks a template AST (ProseMirror/TipTap JSON) and produces a DOCX byte array via docx4j.
 *
 * <p>Node coverage:
 *
 * <ul>
 *   <li>{@code doc} / {@code fragment} — root containers, walk children.
 *   <li>{@code paragraph} — emit {@code W:p} with inline runs.
 *   <li>{@code heading} — {@code W:p} with {@code pStyle=HeadingN}.
 *   <li>{@code text} — {@code W:r/W:t} with raw text.
 *   <li>{@code variable} — resolve {@code attrs.path} via data, emit as text.
 *   <li>{@code conditionBlock} — evaluate {@code attrs.when}; recurse if truthy.
 *   <li>{@code repeatBlock} — evaluate {@code attrs.in} to a list, bind each item to
 *       {@code attrs.each} in a scoped data context, recurse per item.
 * </ul>
 *
 * <p>{@code clauseRef} is not handled here; assembly-service expands those before calling the
 * renderer.
 */
@Component
public class DocxRenderer {

    private final ExpressionEvaluator expressions;
    private final ObjectMapper mapper;
    private final ObjectFactory factory = Context.getWmlObjectFactory();

    public DocxRenderer(ExpressionEvaluator expressions, ObjectMapper mapper) {
        this.expressions = expressions;
        this.mapper = mapper;
    }

    public byte[] render(JsonNode ast, Map<String, Object> data) {
        try {
            WordprocessingMLPackage pkg = WordprocessingMLPackage.createPackage();
            MainDocumentPart doc = pkg.getMainDocumentPart();

            JsonNode children = ast.path("content");
            if (children.isArray()) {
                walkBlocks(children, data, doc);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            pkg.save(out);
            return out.toByteArray();
        } catch (DomainException e) {
            throw e;
        } catch (Exception e) {
            throw new DomainException("rendering.failed", "DOCX rendering failed: " + e.getMessage(), e);
        }
    }

    private void walkBlocks(JsonNode blocks, Map<String, Object> data, MainDocumentPart doc) {
        Iterator<JsonNode> it = blocks.elements();
        while (it.hasNext()) {
            JsonNode node = it.next();
            switch (node.path("type").asText("")) {
                case "paragraph" -> doc.getContent().add(renderParagraph(node, data, null));
                case "heading" -> {
                    int level = Math.max(1, Math.min(6, node.path("attrs").path("level").asInt(1)));
                    doc.getContent().add(renderParagraph(node, data, "Heading" + level));
                }
                case "conditionBlock" -> {
                    String when = node.path("attrs").path("when").asText("");
                    if (!when.isBlank() && expressions.evaluateBoolean(when, data)) {
                        JsonNode innerBlocks = node.path("content");
                        if (innerBlocks.isArray()) walkBlocks(innerBlocks, data, doc);
                    }
                }
                case "repeatBlock" -> renderRepeatBlock(node, data, doc);
                default -> {
                    // Unknown block types are silently ignored.
                }
            }
        }
    }

    private P renderParagraph(JsonNode paragraphNode, Map<String, Object> data, String pStyle) {
        P p = factory.createP();
        if (pStyle != null) {
            PPr pPr = factory.createPPr();
            PPrBase.PStyle s = factory.createPPrBasePStyle();
            s.setVal(pStyle);
            pPr.setPStyle(s);
            p.setPPr(pPr);
        }

        JsonNode inlines = paragraphNode.path("content");
        if (inlines.isArray()) {
            Iterator<JsonNode> it = inlines.elements();
            while (it.hasNext()) {
                JsonNode inline = it.next();
                String type = inline.path("type").asText("");
                switch (type) {
                    case "text" -> p.getContent().add(textRun(inline.path("text").asText("")));
                    case "variable" -> {
                        String path = inline.path("attrs").path("path").asText("");
                        Object resolved = resolvePath(path, data);
                        p.getContent().add(textRun(stringify(resolved)));
                    }
                    default -> { /* skip unsupported inline */ }
                }
            }
        }
        return p;
    }

    private void renderRepeatBlock(JsonNode node, Map<String, Object> data, MainDocumentPart doc) {
        String each = node.path("attrs").path("each").asText("");
        String inExpr = node.path("attrs").path("in").asText("");
        if (each.isBlank() || inExpr.isBlank()) return;

        Object value = expressions.evaluate(inExpr, data);
        if (!(value instanceof List<?> list)) return; // missing / wrong type → silently skip

        JsonNode innerBlocks = node.path("content");
        if (!innerBlocks.isArray()) return;

        for (Object item : list) {
            // Shallow copy of parent scope, shadowed by the loop variable.
            Map<String, Object> scoped = new HashMap<>(data);
            scoped.put(each, item);
            walkBlocks(innerBlocks, scoped, doc);
        }
    }

    private R textRun(String value) {
        R run = factory.createR();
        Text t = factory.createText();
        t.setValue(value == null ? "" : value);
        // Preserve leading/trailing whitespace.
        t.setSpace("preserve");
        run.getContent().add(factory.createRT(t));
        return run;
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

    private String stringify(Object v) {
        if (v == null) return "";
        if (v instanceof String s) return s;
        if (v instanceof Number || v instanceof Boolean) return v.toString();
        try {
            return mapper.writeValueAsString(v);
        } catch (Exception e) {
            return String.valueOf(v);
        }
    }
}
