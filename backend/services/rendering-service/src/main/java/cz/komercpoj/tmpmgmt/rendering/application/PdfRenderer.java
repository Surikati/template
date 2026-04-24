package cz.komercpoj.tmpmgmt.rendering.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import cz.komercpoj.tmpmgmt.common.DomainException;
import cz.komercpoj.tmpmgmt.rendering.config.RenderingProperties;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * PDF renderer — composes the same AST walker output as {@link HtmlRenderer}, wraps it in a
 * minimal XHTML 1.0 document, and converts to PDF via OpenHTMLToPDF (PDFBox backend).
 *
 * <p>If a Unicode TTF font path is configured via {@link RenderingProperties#pdfFontPath()},
 * it's registered with the PDF renderer and used as the body font (essential for Czech
 * diacritics — PDFBox built-in fonts only cover Latin-1). Without a font path, generation
 * falls back to defaults; diacritics may render as empty boxes.
 *
 * <p>No external resources are fetched at runtime — the wrapped HTML uses inline styles only
 * and the builder runs in fast-mode.
 */
@Component
public class PdfRenderer {

    private static final Logger log = LoggerFactory.getLogger(PdfRenderer.class);

    private static final String XHTML_EPILOG = "</body></html>";

    private final HtmlRenderer htmlRenderer;
    private final RenderingProperties props;
    private final File registeredFont;

    public PdfRenderer(HtmlRenderer htmlRenderer, RenderingProperties props) {
        this.htmlRenderer = htmlRenderer;
        this.props = props;
        this.registeredFont = resolveFont(props.pdfFontPath());
        if (props.pdfFontPath() != null && registeredFont == null) {
            log.warn(
                    "PDF font path configured ({}) but file not found — falling back to PDFBox defaults."
                            + " Czech diacritics may render as empty boxes.",
                    props.pdfFontPath());
        } else if (registeredFont != null) {
            log.info("PDF font registered: {} ({})", props.pdfFontFamily(), registeredFont.getAbsolutePath());
        }
    }

    public byte[] render(JsonNode ast, Map<String, Object> data) {
        String htmlBody = htmlRenderer.render(ast, data);
        String xhtml = buildXhtmlProlog() + htmlBody + XHTML_EPILOG;
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            if (registeredFont != null) {
                // null weight/style → builder picks defaults; family name must match the CSS.
                builder.useFont(registeredFont, props.pdfFontFamily());
            }
            builder.withHtmlContent(xhtml, null);
            builder.toStream(os);
            builder.run();
            return os.toByteArray();
        } catch (Exception e) {
            throw new DomainException(
                    "rendering.pdf_failed", "PDF generation failed: " + e.getMessage(), e);
        }
    }

    private String buildXhtmlProlog() {
        // CSS font-family chain: configured family first, then generic fallback so PDFBox can
        // still render something even if the registered font is missing a glyph.
        String fontFamily = "'" + props.pdfFontFamily() + "', sans-serif";
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<html xmlns=\"http://www.w3.org/1999/xhtml\">"
                + "<head>"
                + "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/>"
                + "<title>Document</title>"
                + "<style>"
                + "@page { size: A4; margin: 25mm 22mm; }"
                + "body { font-family: " + fontFamily + "; font-size: 11pt;"
                + " line-height: 1.5; color: #1f2937; }"
                + "h1 { font-size: 18pt; margin: 0 0 12pt; }"
                + "h2 { font-size: 14pt; margin: 12pt 0 6pt; }"
                + "h3 { font-size: 12pt; margin: 10pt 0 4pt; }"
                + "p { margin: 0 0 6pt; }"
                + ".tmpmgmt-preview { padding: 0 !important; max-width: none !important;"
                + " margin: 0 !important; background: transparent !important; }"
                + "</style>"
                + "</head><body>";
    }

    private static File resolveFont(String path) {
        if (path == null || path.isBlank()) return null;
        File f = new File(path);
        return f.exists() && f.isFile() ? f : null;
    }
}
