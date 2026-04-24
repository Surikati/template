package cz.komercpoj.tmpmgmt.template.application.events;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Event payloads staged to the outbox. {@link TemplateCreated} carries the full set of indexable
 * metadata (description, category, tags) so downstream consumers like search-service can build
 * their projection without a follow-up API fetch. Heavy fields (template content, variables
 * schema) stay out — consumers that need them call template-service directly.
 */
public final class TemplateEvents {

    private TemplateEvents() {}

    public static final String AGGREGATE_TYPE = "template";

    public static final String TYPE_CREATED = "created";
    public static final String TYPE_METADATA_UPDATED = "metadata.updated";
    public static final String TYPE_DRAFT_SAVED = "draft.saved";
    public static final String TYPE_VERSION_PUBLISHED = "version.published";
    public static final String TYPE_ARCHIVED = "archived";

    public record TemplateCreated(
            UUID templateId,
            String slug,
            String name,
            String description,
            String category,
            List<String> tags,
            UUID ownerUserId,
            Instant occurredAt) {}

    /**
     * Emitted when name/description/category/tags change. Slug stays out — it's part of the
     * template's identity and is intentionally immutable post-creation.
     */
    public record TemplateMetadataUpdated(
            UUID templateId,
            String name,
            String description,
            String category,
            List<String> tags,
            UUID actorUserId,
            Instant occurredAt) {}

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
