package cz.komercpoj.tmpmgmt.search.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.komercpoj.tmpmgmt.search.config.OpenSearchConfig;
import cz.komercpoj.tmpmgmt.search.domain.SearchableDocument;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.UpdateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Listens for template and clause domain events and keeps the OpenSearch indices in sync.
 * Field coverage is limited to what events carry (id, slug, name, status). Richer fields
 * like description/category would need a follow-up fetch against the owning service or wider
 * event payloads — deferred.
 */
@Component
public class SearchIndexer {

    private static final Logger log = LoggerFactory.getLogger(SearchIndexer.class);

    private final OpenSearchClient client;
    private final ObjectMapper mapper;

    public SearchIndexer(OpenSearchClient client, ObjectMapper mapper) {
        this.client = client;
        this.mapper = mapper;
    }

    @RabbitListener(queues = "#{templateSearchQueue.name}")
    public void onTemplateEvent(Message message) {
        handle(message, OpenSearchConfig.INDEX_TEMPLATE, "templateId");
    }

    @RabbitListener(queues = "#{clauseSearchQueue.name}")
    public void onClauseEvent(Message message) {
        handle(message, OpenSearchConfig.INDEX_CLAUSE, "clauseId");
    }

    private void handle(Message message, String index, String idField) {
        String eventType = String.valueOf(message.getMessageProperties().getHeaders().get("event-type"));
        JsonNode payload;
        try {
            payload = mapper.readTree(new String(message.getBody(), StandardCharsets.UTF_8));
        } catch (Exception ex) {
            log.warn("Skipping event — invalid JSON payload: {}", ex.getMessage());
            return;
        }

        UUID id;
        try {
            id = UUID.fromString(payload.path(idField).asText());
        } catch (IllegalArgumentException ex) {
            log.warn("Skipping {} event — missing/invalid {}", eventType, idField);
            return;
        }

        try {
            switch (eventType) {
                case "created" -> upsertFromCreated(index, id, payload);
                case "version.published" -> touchUpdated(index, id, payload);
                case "archived" -> setStatus(index, id, "ARCHIVED", payload);
                default -> log.debug("Ignoring event type '{}' for {}", eventType, index);
            }
        } catch (Exception ex) {
            log.error("Failed to index {} event {} for id {}", index, eventType, id, ex);
        }
    }

    private void upsertFromCreated(String index, UUID id, JsonNode payload) throws Exception {
        SearchableDocument doc = new SearchableDocument(
                id,
                payload.path("slug").asText(null),
                payload.path("name").asText(null),
                null,                                 // description — not in event
                null,                                 // category — not in event
                "ACTIVE",
                occurredAt(payload));
        client.update(UpdateRequest.of(u -> u
                .index(index)
                .id(id.toString())
                .doc(doc)
                .docAsUpsert(true)), SearchableDocument.class);
    }

    private void touchUpdated(String index, UUID id, JsonNode payload) throws Exception {
        Map<String, Object> partial = Map.of("updatedAt", occurredAt(payload));
        client.update(UpdateRequest.of(u -> u
                .index(index)
                .id(id.toString())
                .doc(partial)
                .docAsUpsert(false)), Map.class);
    }

    private void setStatus(String index, UUID id, String status, JsonNode payload) throws Exception {
        Map<String, Object> partial = Map.of(
                "status", status,
                "updatedAt", occurredAt(payload));
        client.update(UpdateRequest.of(u -> u
                .index(index)
                .id(id.toString())
                .doc(partial)
                .docAsUpsert(false)), Map.class);
    }

    private static Instant occurredAt(JsonNode payload) {
        JsonNode ts = payload.path("occurredAt");
        if (ts.isTextual()) {
            try {
                return Instant.parse(ts.asText());
            } catch (Exception ignored) { /* fall through */ }
        }
        return Instant.now();
    }
}
