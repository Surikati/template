package cz.komercpoj.tmpmgmt.questionnaire.api;

import cz.komercpoj.tmpmgmt.questionnaire.api.dto.CreateQuestionnaireRequest;
import cz.komercpoj.tmpmgmt.questionnaire.api.dto.QuestionInputDto;
import cz.komercpoj.tmpmgmt.questionnaire.api.dto.QuestionnaireResponse;
import cz.komercpoj.tmpmgmt.questionnaire.api.dto.QuestionnaireVersionResponse;
import cz.komercpoj.tmpmgmt.questionnaire.api.dto.ReplaceStructureRequest;
import cz.komercpoj.tmpmgmt.questionnaire.api.dto.SectionInputDto;
import cz.komercpoj.tmpmgmt.questionnaire.application.QuestionnaireCommands;
import cz.komercpoj.tmpmgmt.questionnaire.application.QuestionnaireService;
import cz.komercpoj.tmpmgmt.questionnaire.persistence.QuestionnaireVersionEntity;
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
@RequestMapping("/api/v1/questionnaires")
public class QuestionnaireController {

  private final QuestionnaireService service;
  private final QuestionnaireMapper mapper;

  public QuestionnaireController(QuestionnaireService service, QuestionnaireMapper mapper) {
    this.service = service;
    this.mapper = mapper;
  }

  @GetMapping("/{id}")
  public QuestionnaireResponse get(@PathVariable UUID id) {
    return mapper.toResponse(service.getById(id));
  }

  @GetMapping("/by-template-version")
  public ResponseEntity<QuestionnaireResponse> findByTemplate(
      @RequestParam UUID templateId, @RequestParam int versionNumber) {
    return service
        .findByTemplateVersion(templateId, versionNumber)
        .map(q -> ResponseEntity.ok(mapper.toResponse(q)))
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN','TEMPLATE_EDITOR')")
  public ResponseEntity<QuestionnaireResponse> create(
      @Valid @RequestBody CreateQuestionnaireRequest req) {
    var created =
        service.create(
            new QuestionnaireCommands.CreateQuestionnaire(
                req.templateId(),
                req.templateVersionNumber(),
                req.name(),
                toSectionInputs(req.sections())));
    return ResponseEntity.created(URI.create("/api/v1/questionnaires/" + created.getId()))
        .body(mapper.toResponse(created));
  }

  @PutMapping("/{id}/structure")
  @PreAuthorize("hasAnyRole('ADMIN','TEMPLATE_EDITOR')")
  public QuestionnaireResponse replaceStructure(
      @PathVariable UUID id, @Valid @RequestBody ReplaceStructureRequest req) {
    var updated =
        service.replaceStructure(
            new QuestionnaireCommands.ReplaceStructure(
                id, req.name(), toSectionInputs(req.sections())));
    return mapper.toResponse(updated);
  }

  @PostMapping("/{id}/versions")
  @PreAuthorize("hasAnyRole('ADMIN','TEMPLATE_EDITOR')")
  public ResponseEntity<QuestionnaireVersionResponse> publishVersion(
      @PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
    var v =
        service.publishVersion(
            new QuestionnaireCommands.PublishQuestionnaireVersion(
                id, UUID.fromString(jwt.getSubject())));
    return ResponseEntity.created(
            URI.create("/api/v1/questionnaires/" + id + "/versions/" + v.getVersionNumber()))
        .body(toResponse(v));
  }

  @GetMapping("/{id}/versions")
  public List<QuestionnaireVersionResponse> listVersions(@PathVariable UUID id) {
    return service.listVersions(id).stream().map(QuestionnaireController::toResponse).toList();
  }

  @GetMapping("/{id}/versions/{versionNumber}")
  public QuestionnaireVersionResponse getVersion(
      @PathVariable UUID id, @PathVariable int versionNumber) {
    return toResponse(service.getVersion(id, versionNumber));
  }

  private static QuestionnaireVersionResponse toResponse(QuestionnaireVersionEntity v) {
    return new QuestionnaireVersionResponse(
        v.getId(),
        v.getQuestionnaireId(),
        v.getVersionNumber(),
        v.getNameSnapshot(),
        v.getStructureSnapshot(),
        v.getPublishedAt(),
        v.getPublishedBy());
  }

  private List<QuestionnaireCommands.SectionInput> toSectionInputs(List<SectionInputDto> inputs) {
    return inputs.stream()
        .map(
            s ->
                new QuestionnaireCommands.SectionInput(
                    s.ordinal(),
                    s.title(),
                    s.visibilityRule(),
                    s.questions().stream().map(this::toQuestionInput).toList()))
        .toList();
  }

  private QuestionnaireCommands.QuestionInput toQuestionInput(QuestionInputDto q) {
    return new QuestionnaireCommands.QuestionInput(
        q.ordinal(),
        q.variablePath(),
        q.label(),
        q.questionType(),
        q.validation(),
        q.visibilityRule(),
        q.options());
  }
}
