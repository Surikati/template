package cz.komercpoj.tmpmgmt.assembly.api;

import cz.komercpoj.tmpmgmt.assembly.api.dto.AssembleRequest;
import cz.komercpoj.tmpmgmt.assembly.api.dto.AssembleResponse;
import cz.komercpoj.tmpmgmt.assembly.application.AssemblyService;
import cz.komercpoj.tmpmgmt.assembly.application.AssemblyService.AssemblyCommand;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/assemblies")
public class AssemblyController {

    private final AssemblyService service;

    public AssemblyController(AssemblyService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<AssembleResponse> assemble(
            @Valid @RequestBody AssembleRequest req, @AuthenticationPrincipal Jwt jwt) {
        var result = service.assemble(new AssemblyCommand(
                req.templateId(),
                req.templateVersionNumber(),
                req.data(),
                AssemblyCommand.Format.DOCX,
                currentUserId(jwt)));
        var job = result.job();
        var body = new AssembleResponse(
                job.getId(),
                job.getState(),
                result.documentId(),
                result.filename(),
                result.downloadUrl(),
                job.getCompletedAt());
        return ResponseEntity.created(URI.create("/api/v1/assemblies/" + job.getId())).body(body);
    }

    @PostMapping(value = "/preview", produces = MediaType.TEXT_HTML_VALUE)
    public String preview(@Valid @RequestBody AssembleRequest req, @AuthenticationPrincipal Jwt jwt) {
        return service.preview(new AssemblyCommand(
                req.templateId(),
                req.templateVersionNumber(),
                req.data(),
                AssemblyCommand.Format.DOCX,
                currentUserId(jwt)));
    }

    @GetMapping("/{id}")
    public AssembleResponse get(@PathVariable UUID id) {
        var job = service.getById(id);
        return new AssembleResponse(
                job.getId(),
                job.getState(),
                job.getResultDocumentId(),
                null,
                null,
                job.getCompletedAt());
    }

    private UUID currentUserId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
