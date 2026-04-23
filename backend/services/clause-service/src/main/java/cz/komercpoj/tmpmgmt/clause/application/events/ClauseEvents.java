package cz.komercpoj.tmpmgmt.clause.application.events;

import java.time.Instant;
import java.util.UUID;

public final class ClauseEvents {

    private ClauseEvents() {}

    public static final String AGGREGATE_TYPE = "clause";

    public static final String TYPE_CREATED = "created";
    public static final String TYPE_VERSION_PUBLISHED = "version.published";
    public static final String TYPE_ARCHIVED = "archived";

    public record ClauseCreated(
            UUID clauseId, String slug, String name, UUID ownerUserId, Instant occurredAt) {}

    public record ClauseVersionPublished(
            UUID clauseId,
            UUID versionId,
            int versionNumber,
            String changeNote,
            UUID publishedBy,
            Instant occurredAt) {}

    public record ClauseArchived(UUID clauseId, UUID actorUserId, Instant occurredAt) {}
}
