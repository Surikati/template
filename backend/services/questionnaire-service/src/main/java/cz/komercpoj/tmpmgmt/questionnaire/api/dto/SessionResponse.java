package cz.komercpoj.tmpmgmt.questionnaire.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import cz.komercpoj.tmpmgmt.questionnaire.domain.SessionState;
import java.time.Instant;
import java.util.UUID;

public record SessionResponse(
    UUID id,
    UUID questionnaireId,
    SessionState state,
    UUID startedBy,
    Instant startedAt,
    Instant completedAt,
    JsonNode answers,
    UUID currentSectionId) {}
