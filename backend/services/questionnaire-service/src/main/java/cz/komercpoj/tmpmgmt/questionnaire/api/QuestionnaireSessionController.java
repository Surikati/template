package cz.komercpoj.tmpmgmt.questionnaire.api;

import cz.komercpoj.tmpmgmt.questionnaire.api.dto.SessionResponse;
import cz.komercpoj.tmpmgmt.questionnaire.api.dto.StartSessionRequest;
import cz.komercpoj.tmpmgmt.questionnaire.api.dto.SubmitAnswersRequest;
import cz.komercpoj.tmpmgmt.questionnaire.application.QuestionnaireCommands;
import cz.komercpoj.tmpmgmt.questionnaire.application.SessionService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/questionnaire-sessions")
public class QuestionnaireSessionController {

  private final SessionService service;
  private final QuestionnaireMapper mapper;

  public QuestionnaireSessionController(SessionService service, QuestionnaireMapper mapper) {
    this.service = service;
    this.mapper = mapper;
  }

  @GetMapping("/{id}")
  public SessionResponse get(@PathVariable UUID id) {
    return mapper.toResponse(service.getById(id));
  }

  @PostMapping
  public ResponseEntity<SessionResponse> start(
      @Valid @RequestBody StartSessionRequest req, @AuthenticationPrincipal Jwt jwt) {
    var session =
        service.start(
            new QuestionnaireCommands.StartSession(req.questionnaireId(), currentUserId(jwt)));
    return ResponseEntity.created(URI.create("/api/v1/questionnaire-sessions/" + session.getId()))
        .body(mapper.toResponse(session));
  }

  @PostMapping("/{id}/answers")
  public SessionResponse submitAnswers(
      @PathVariable UUID id, @Valid @RequestBody SubmitAnswersRequest req) {
    return mapper.toResponse(
        service.submitAnswers(new QuestionnaireCommands.SubmitAnswers(id, req.answers())));
  }

  @PostMapping("/{id}/complete")
  public SessionResponse complete(@PathVariable UUID id) {
    return mapper.toResponse(service.complete(new QuestionnaireCommands.CompleteSession(id)));
  }

  private UUID currentUserId(Jwt jwt) {
    return UUID.fromString(jwt.getSubject());
  }
}
