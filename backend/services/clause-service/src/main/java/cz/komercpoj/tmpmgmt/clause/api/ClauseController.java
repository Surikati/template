package cz.komercpoj.tmpmgmt.clause.api;

import cz.komercpoj.tmpmgmt.clause.api.dto.ClauseResponse;
import cz.komercpoj.tmpmgmt.clause.api.dto.ClauseVersionResponse;
import cz.komercpoj.tmpmgmt.clause.api.dto.CreateClauseRequest;
import cz.komercpoj.tmpmgmt.clause.api.dto.PublishClauseVersionRequest;
import cz.komercpoj.tmpmgmt.clause.api.dto.UpdateMetadataRequest;
import cz.komercpoj.tmpmgmt.clause.application.ClauseCommands;
import cz.komercpoj.tmpmgmt.clause.application.ClauseService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/clauses")
public class ClauseController {

  private final ClauseService service;
  private final ClauseMapper mapper;

  public ClauseController(ClauseService service, ClauseMapper mapper) {
    this.service = service;
    this.mapper = mapper;
  }

  @GetMapping
  public List<ClauseResponse> list() {
    return mapper.toClauseResponses(service.list());
  }

  @GetMapping("/{id}")
  public ClauseResponse get(@PathVariable UUID id) {
    return mapper.toResponse(service.getById(id));
  }

  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN','CLAUSE_EDITOR')")
  public ResponseEntity<ClauseResponse> create(
      @Valid @RequestBody CreateClauseRequest req, @AuthenticationPrincipal Jwt jwt) {
    var created =
        service.create(
            new ClauseCommands.CreateClause(
                req.slug(), req.name(), req.description(), req.category(), currentUserId(jwt)));
    return ResponseEntity.created(URI.create("/api/v1/clauses/" + created.getId()))
        .body(mapper.toResponse(created));
  }

  @PutMapping("/{id}/metadata")
  @PreAuthorize("hasAnyRole('ADMIN','CLAUSE_EDITOR')")
  public ClauseResponse updateMetadata(
      @PathVariable UUID id,
      @Valid @RequestBody UpdateMetadataRequest req,
      @AuthenticationPrincipal Jwt jwt) {
    var updated =
        service.updateMetadata(
            new ClauseCommands.UpdateMetadata(
                id, req.name(), req.description(), req.category(), req.tags(), currentUserId(jwt)));
    return mapper.toResponse(updated);
  }

  @GetMapping("/{id}/versions")
  public List<ClauseVersionResponse> listVersions(@PathVariable UUID id) {
    return mapper.toVersionResponses(service.listVersions(id));
  }

  @GetMapping("/{id}/versions/{versionNumber}")
  public ClauseVersionResponse getVersion(@PathVariable UUID id, @PathVariable int versionNumber) {
    return mapper.toResponse(service.getVersion(id, versionNumber));
  }

  @PostMapping("/{id}/versions")
  @PreAuthorize("hasAnyRole('ADMIN','CLAUSE_EDITOR')")
  public ResponseEntity<ClauseVersionResponse> publishVersion(
      @PathVariable UUID id,
      @Valid @RequestBody PublishClauseVersionRequest req,
      @AuthenticationPrincipal Jwt jwt) {
    var published =
        service.publishVersion(
            new ClauseCommands.PublishVersion(
                id, req.content(), req.changeNote(), currentUserId(jwt)));
    return ResponseEntity.created(
            URI.create("/api/v1/clauses/" + id + "/versions/" + published.getVersionNumber()))
        .body(mapper.toResponse(published));
  }

  @PostMapping("/{id}/archive")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> archive(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
    service.archive(new ClauseCommands.Archive(id, currentUserId(jwt)));
    return ResponseEntity.noContent().build();
  }

  private UUID currentUserId(Jwt jwt) {
    return UUID.fromString(jwt.getSubject());
  }
}
