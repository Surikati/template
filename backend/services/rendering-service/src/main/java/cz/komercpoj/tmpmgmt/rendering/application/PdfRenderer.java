package cz.komercpoj.tmpmgmt.rendering.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import cz.komercpoj.tmpmgmt.common.DomainException;
import java.io.ByteArrayOutputStream;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * PDF renderer — composes the same AST walker output as {@link HtmlRenderer}, wraps it in a
 * minimal XHTML 1.0 document, and converts to PDF via OpenHTMLToPDF (PDFBox backend).
 *
 * <p>No external resources are fetched — the wrapped HTML uses inline styles only and the
 * builder runs in fast-mode. Default fonts come from PDFBox's built-in fallbacks; for
 * production-quality Czech diacritics, register a Unicode TTF (e.g. DejaVuSans) via
 * {@code PdfRendererBuilder.useFont(...)} — left as a follow-up since it requires bundling a
 * binary font asset.
 */
@Component
public class PdfRenderer {

    private static final String XHTML_PROLOG =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    + "<html xmlns=\"http://www.w3.org/1999/xhtml\">"
                    + "<head>"
                    + "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/>"
                    + "<title>Document</title>"
                    + "<style>"
                    + "@page { size: A4; margin: 25mm 22mm; }"
                    + "body { font-family: 'Helvetica', sans-serif; font-size: 11pt;"
                    + " line-height: 1.5; color: #1f2937; }"
                    + "h1 { font-size: 18pt; margin: 0 0 12pt; }"
                    + "h2 { font-size: 14pt; margin: 12pt 0 6pt; }"
                    + "h3 { font-size: 12pt; margin: 10pt 0 4pt; }"
                    + "p { margin: 0 0 6pt; }"
                    + ".tmpmgmt-preview { padding: 0 !important; max-width: none !important;"
                    + " margin: 0 !important; background: transparent !important; }"
                    + "</style>"
                    + "</head><body>";

    private static final String XHTML_EPILOG = "</body></html>";

    private final HtmlRenderer htmlRenderer;

    public PdfRenderer(HtmlRenderer htmlRenderer) {
        this.htmlRenderer = htmlRenderer;
    }

    public byte[] render(JsonNode ast, Map<String, Object> data) {
        String htmlBody = htmlRenderer.render(ast, data);
        String xhtml = XHTML_PROLOG + htmlBody + XHTML_EPILOG;
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(xhtml, null);
            builder.toStream(os);
            builder.run();
            return os.toByteArray();
        } catch (Exception e) {
            throw new DomainException(
                    "rendering.pdf_failed", "PDF generation failed: " + e.getMessage(), e);
        }
    }
}
