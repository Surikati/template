package cz.komercpoj.tmpmgmt.rendering.api;

import cz.komercpoj.tmpmgmt.common.DomainException;
import cz.komercpoj.tmpmgmt.rendering.api.dto.RenderFormat;
import cz.komercpoj.tmpmgmt.rendering.api.dto.RenderRequest;
import cz.komercpoj.tmpmgmt.rendering.api.dto.RenderResponse;
import cz.komercpoj.tmpmgmt.rendering.application.DocxRenderer;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/render")
public class RenderingController {

    private final DocxRenderer docxRenderer;

    public RenderingController(DocxRenderer docxRenderer) {
        this.docxRenderer = docxRenderer;
    }

    @PostMapping
    public RenderResponse render(@Valid @RequestBody RenderRequest req) {
        if (req.format() != RenderFormat.DOCX) {
            throw new DomainException(
                    "rendering.format_unsupported",
                    "Format " + req.format() + " is not yet implemented (only DOCX for now).");
        }
        byte[] content = docxRenderer.render(req.content(), req.data());
        return new RenderResponse(RenderFormat.DOCX, "document.docx", content);
    }
}
