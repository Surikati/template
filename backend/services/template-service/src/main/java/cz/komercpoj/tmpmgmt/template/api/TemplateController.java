package cz.komercpoj.tmpmgmt.template.api;

import cz.komercpoj.tmpmgmt.template.api.dto.CreateTemplateRequest;
import cz.komercpoj.tmpmgmt.template.api.dto.PublishVersionRequest;
import cz.komercpoj.tmpmgmt.template.api.dto.TemplateDraftResponse;
import cz.komercpoj.tmpmgmt.template.api.dto.TemplateResponse;
import cz.komercpoj.tmpmgmt.template.api.dto.TemplateVersionResponse;
import cz.komercpoj.tmpmgmt.template.api.dto.UpdateDraftRequest;
import cz.komercpoj.tmpmgmt.template.api.dto.UpdateMetadataRequest;
import cz.komercpoj.tmpmgmt.template.application.TemplateCommands;
import cz.komercpoj.tmpmgmt.template.application.TemplateService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/templates")
public class TemplateController {

    private final TemplateService service;
    private final TemplateMapper mapper;

    public TemplateController(TemplateService service, TemplateMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping
    public List<TemplateResponse> list() {
        return mapper.toTemplateResponses(service.list());
    }

    @GetMapping("/{id}")
    public TemplateResponse get(@PathVariable UUID id) {
        return mapper.toResponse(service.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEMPLATE_EDITOR')")
    public ResponseEntity<TemplateResponse> create(
            @Valid @RequestBody CreateTemplateRequest req,
            @org.springframework.security.core.annotation.AuthenticationPrincipal Jwt jwt) {
        UUID userId = currentUserId(jwt);
        var created = service.create(new TemplateCommands.CreateTemplate(
                req.slug(), req.name(), req.description(), req.category(), userId));
        return ResponseEntity.created(URI.create("/api/v1/templates/" + created.getId()))
                .body(mapper.toResponse(created));
    }

    @PutMapping("/{id}/metadata")
    @PreAuthorize("hasAnyRole('ADMIN','TEMPLATE_EDITOR')")
    public TemplateResponse updateMetadata(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateMetadataRequest req,
            @org.springframework.security.core.annotation.AuthenticationPrincipal Jwt jwt) {
        var updated = service.updateMetadata(new TemplateCommands.UpdateMetadata(
                id, req.name(), req.description(), req.category(), req.tags(), currentUserId(jwt)));
        return mapper.toResponse(updated);
    }

    @GetMapping("/{id}/draft")
    public TemplateDraftResponse getDraft(@PathVariable UUID id) {
        return mapper.toResponse(service.getDraft(id));
    }

    @PutMapping("/{id}/draft")
    @PreAuthorize("hasAnyRole('ADMIN','TEMPLATE_EDITOR')")
    public TemplateDraftResponse saveDraft(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateDraftRequest req,
            @org.springframework.security.core.annotation.AuthenticationPrincipal Jwt jwt) {
        var saved = service.saveDraft(new TemplateCommands.SaveDraft(
                id, req.content(), req.variablesSchema(), currentUserId(jwt)));
        return mapper.toResponse(saved);
    }

    @GetMapping("/{id}/versions")
    public List<TemplateVersionResponse> listVersions(@PathVariable UUID id) {
        return mapper.toVersionResponses(service.listVersions(id));
    }

    @PostMapping("/{id}/versions")
    @PreAuthorize("hasAnyRole('ADMIN','TEMPLATE_EDITOR')")
    public ResponseEntity<TemplateVersionResponse> publishVersion(
            @PathVariable UUID id,
            @Valid @RequestBody PublishVersionRequest req,
            @org.springframework.security.core.annotation.AuthenticationPrincipal Jwt jwt) {
        var published = service.publishVersion(
                new TemplateCommands.PublishVersion(id, req.changeNote(), currentUserId(jwt)));
        return ResponseEntity.created(URI.create(
                        "/api/v1/templates/" + id + "/versions/" + published.getVersionNumber()))
                .body(mapper.toResponse(published));
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> archive(
            @PathVariable UUID id,
            @org.springframework.security.core.annotation.AuthenticationPrincipal Jwt jwt) {
        service.archive(new TemplateCommands.Archive(id, currentUserId(jwt)));
        return ResponseEntity.noContent().build();
    }

    private UUID currentUserId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
