package cz.komercpoj.tmpmgmt.template.application.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Event payloads staged to the outbox. Kept lean (IDs + metadata, no full content) so RabbitMQ
 * messages stay small and consumers pull full state via the API when they need it.
 */
public final class TemplateEvents {

    private TemplateEvents() {}

    public static final String AGGREGATE_TYPE = "template";

    public static final String TYPE_CREATED = "created";
    public static final String TYPE_DRAFT_SAVED = "draft.saved";
    public static final String TYPE_VERSION_PUBLISHED = "version.published";
    public static final String TYPE_ARCHIVED = "archived";

    public record TemplateCreated(
            UUID templateId, String slug, String name, UUID ownerUserId, Instant occurredAt) {}

    public record TemplateDraftSaved(
            UUID templateId, UUID editorUserId, Instant occurredAt) {}

    public record TemplateVersionPublished(
            UUID templateId,
            UUID versionId,
            int versionNumber,
            String changeNote,
            UUID publishedBy,
            Instant occurredAt) {}

    public record TemplateArchived(UUID templateId, UUID actorUserId, Instant occurredAt) {}
}
