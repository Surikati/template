package cz.komercpoj.tmpmgmt.audit;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

import tools.jackson.databind.ObjectMapper;
import cz.komercpoj.tmpmgmt.audit.persistence.AuditEventRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Posts messages straight to the exchange (simulating upstream services) and asserts that the
 * consumer persists them and the query repo surfaces them back.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import({TestcontainersConfig.class, TestSecurityConfig.class})
class AuditConsumerIT {

  @Autowired RabbitTemplate rabbit;
  @Autowired TopicExchange auditExchange;
  @Autowired AuditEventRepository repo;
  @Autowired ObjectMapper mapper;

  @Test
  void consumesEventFromExchange_storesAndQuery() throws Exception {
    UUID eventId = UUID.randomUUID();
    UUID actor = UUID.randomUUID();
    UUID templateId = UUID.randomUUID();
    Instant occurredAt = Instant.parse("2026-04-23T10:00:00Z");

    var payload =
        Map.of(
            "templateId",
            templateId,
            "slug",
            "nda-standard",
            "ownerUserId",
            actor,
            "occurredAt",
            occurredAt.toString());

    rabbit.convertAndSend(
        auditExchange.getName(),
        "template.created",
        mapper.writeValueAsString(payload),
        m -> {
          m.getMessageProperties().setMessageId(eventId.toString());
          m.getMessageProperties().setContentType(MessageProperties.CONTENT_TYPE_JSON);
          m.getMessageProperties().setHeader("event-type", "created");
          m.getMessageProperties().setHeader("aggregate-type", "template");
          m.getMessageProperties().setHeader("aggregate-id", templateId.toString());
          return m;
        });

    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(
            () -> {
              List<?> events =
                  repo.findByAggregate(
                      "template",
                      templateId.toString(),
                      org.springframework.data.domain.PageRequest.of(0, 10));
              assertThat(events).hasSize(1);
            });

    // Verify fields
    var saved =
        repo.findByAggregate(
                "template",
                templateId.toString(),
                org.springframework.data.domain.PageRequest.of(0, 1))
            .get(0);
    assertThat(saved.getId().getEventId()).isEqualTo(eventId);
    assertThat(saved.getEventType()).isEqualTo("created");
    assertThat(saved.getActorUserId()).isEqualTo(actor);
    assertThat(saved.getPayload().get("slug").asText()).isEqualTo("nda-standard");
  }

  @Test
  void duplicateMessage_isIdempotent() throws Exception {
    UUID eventId = UUID.randomUUID();
    UUID clauseId = UUID.randomUUID();
    Instant occurredAt = Instant.parse("2026-04-23T11:00:00Z");

    var payload = Map.of("clauseId", clauseId, "occurredAt", occurredAt.toString());
    var json = mapper.writeValueAsString(payload);

    // Publish same event twice
    for (int i = 0; i < 2; i++) {
      rabbit.convertAndSend(
          auditExchange.getName(),
          "clause.created",
          json,
          m -> {
            m.getMessageProperties().setMessageId(eventId.toString());
            m.getMessageProperties().setHeader("event-type", "created");
            m.getMessageProperties().setHeader("aggregate-type", "clause");
            m.getMessageProperties().setHeader("aggregate-id", clauseId.toString());
            return m;
          });
    }

    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(
            () ->
                assertThat(
                        repo.findByAggregate(
                            "clause",
                            clauseId.toString(),
                            org.springframework.data.domain.PageRequest.of(0, 10)))
                    .hasSize(1));
  }
}
