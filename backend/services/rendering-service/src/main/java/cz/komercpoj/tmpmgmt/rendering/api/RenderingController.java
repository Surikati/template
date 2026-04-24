package cz.komercpoj.tmpmgmt.rendering.api;

import cz.komercpoj.tmpmgmt.rendering.api.dto.RenderFormat;
import cz.komercpoj.tmpmgmt.rendering.api.dto.RenderRequest;
import cz.komercpoj.tmpmgmt.rendering.api.dto.RenderResponse;
import cz.komercpoj.tmpmgmt.rendering.application.DocxRenderer;
import cz.komercpoj.tmpmgmt.rendering.application.HtmlRenderer;
import cz.komercpoj.tmpmgmt.rendering.application.PdfRenderer;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/render")
public class RenderingController {

    private final DocxRenderer docxRenderer;
    private final HtmlRenderer htmlRenderer;
    private final PdfRenderer pdfRenderer;

    public RenderingController(
            DocxRenderer docxRenderer, HtmlRenderer htmlRenderer, PdfRenderer pdfRenderer) {
        this.docxRenderer = docxRenderer;
        this.htmlRenderer = htmlRenderer;
        this.pdfRenderer = pdfRenderer;
    }

    @PostMapping
    public RenderResponse render(@Valid @RequestBody RenderRequest req) {
        return switch (req.format()) {
            case DOCX -> new RenderResponse(
                    RenderFormat.DOCX, "document.docx",
                    docxRenderer.render(req.content(), req.data()));
            case PDF -> new RenderResponse(
                    RenderFormat.PDF, "document.pdf",
                    pdfRenderer.render(req.content(), req.data()));
            case HTML -> new RenderResponse(
                    RenderFormat.HTML, "preview.html",
                    htmlRenderer.render(req.content(), req.data()).getBytes(StandardCharsets.UTF_8));
        };
    }
}
