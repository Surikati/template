package cz.komercpoj.tmpmgmt.assembly.application.events;

import java.time.Instant;
import java.util.UUID;

public final class AssemblyEvents {

    private AssemblyEvents() {}

    public static final String AGGREGATE_TYPE = "assembly";
    public static final String TYPE_REQUESTED = "requested";
    public static final String TYPE_COMPLETED = "completed";
    public static final String TYPE_FAILED = "failed";

    public record AssemblyRequested(
            UUID jobId, UUID templateId, int templateVersionNumber, UUID requestedBy, Instant occurredAt) {}

    public record AssemblyCompleted(
            UUID jobId, UUID templateId, int templateVersionNumber, Instant occurredAt) {}

    public record AssemblyFailed(
            UUID jobId, String errorCode, String errorMessage, Instant occurredAt) {}
}
