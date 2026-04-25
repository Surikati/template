package cz.komercpoj.tmpmgmt.questionnaire.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record QuestionnaireResponse(
    UUID id,
    UUID templateId,
    int templateVersionNumber,
    String name,
    List<SectionResponse> sections,
    Instant createdAt,
    Instant updatedAt) {}
