package cz.komercpoj.tmpmgmt.rendering.api;

import cz.komercpoj.tmpmgmt.common.DomainException;
import cz.komercpoj.tmpmgmt.rendering.api.dto.RenderFormat;
import cz.komercpoj.tmpmgmt.rendering.api.dto.RenderRequest;
import cz.komercpoj.tmpmgmt.rendering.api.dto.RenderResponse;
import cz.komercpoj.tmpmgmt.rendering.application.DocxRenderer;
import cz.komercpoj.tmpmgmt.rendering.application.HtmlRenderer;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/render")
public class RenderingController {

    private final DocxRenderer docxRenderer;
    private final HtmlRenderer htmlRenderer;

    public RenderingController(DocxRenderer docxRenderer, HtmlRenderer htmlRenderer) {
        this.docxRenderer = docxRenderer;
        this.htmlRenderer = htmlRenderer;
    }

    @PostMapping
    public RenderResponse render(@Valid @RequestBody RenderRequest req) {
        return switch (req.format()) {
            case DOCX -> new RenderResponse(
                    RenderFormat.DOCX, "document.docx",
                    docxRenderer.render(req.content(), req.data()));
            case HTML -> new RenderResponse(
                    RenderFormat.HTML, "preview.html",
                    htmlRenderer.render(req.content(), req.data()).getBytes(StandardCharsets.UTF_8));
        };
    }
}
