package cz.komercpoj.tmpmgmt.clause.application.events;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * See {@link cz.komercpoj.tmpmgmt.template.application.events.TemplateEvents} for the rationale
 * on event-carried state — same pattern: metadata in the event, heavy content fetched on demand.
 */
public final class ClauseEvents {

    private ClauseEvents() {}

    public static final String AGGREGATE_TYPE = "clause";

    public static final String TYPE_CREATED = "created";
    public static final String TYPE_METADATA_UPDATED = "metadata.updated";
    public static final String TYPE_VERSION_PUBLISHED = "version.published";
    public static final String TYPE_ARCHIVED = "archived";

    public record ClauseCreated(
            UUID clauseId,
            String slug,
            String name,
            String description,
            String category,
            List<String> tags,
            UUID ownerUserId,
            Instant occurredAt) {}

    public record ClauseMetadataUpdated(
            UUID clauseId,
            String name,
            String description,
            String category,
            List<String> tags,
            UUID actorUserId,
            Instant occurredAt) {}

    public record ClauseVersionPublished(
            UUID clauseId,
            UUID versionId,
            int versionNumber,
            String changeNote,
            UUID publishedBy,
            Instant occurredAt) {}

    public record ClauseArchived(UUID clauseId, UUID actorUserId, Instant occurredAt) {}
}
