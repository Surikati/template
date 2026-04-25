package cz.komercpoj.tmpmgmt.assembly.application;

import com.fasterxml.jackson.databind.JsonNode;
import cz.komercpoj.tmpmgmt.assembly.application.events.AssemblyEvents;
import cz.komercpoj.tmpmgmt.assembly.persistence.AssemblyJobEntity;
import cz.komercpoj.tmpmgmt.assembly.persistence.AssemblyJobRepository;
import cz.komercpoj.tmpmgmt.outbox.OutboxWriter;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists each lifecycle step of an assembly job in its own transaction so that the FAILED state
 * survives even when the outer assembly orchestrator throws. Lives in a separate bean so Spring's
 * AOP proxy intercepts the {@code @Transactional} calls (self-calls within {@code AssemblyService}
 * would bypass the proxy).
 */
@Service
public class JobLifecycleRecorder {

  private final AssemblyJobRepository jobs;
  private final OutboxWriter outbox;

  public JobLifecycleRecorder(AssemblyJobRepository jobs, OutboxWriter outbox) {
    this.jobs = jobs;
    this.outbox = outbox;
  }

  @Transactional
  public AssemblyJobEntity recordRequested(
      UUID jobId,
      UUID templateId,
      int templateVersionNumber,
      JsonNode inputData,
      String[] formats,
      UUID requestedBy) {
    AssemblyJobEntity job =
        AssemblyJobEntity.pending(
            jobId, templateId, templateVersionNumber, inputData, formats, requestedBy);
    jobs.save(job);
    outbox.stage(
        AssemblyEvents.AGGREGATE_TYPE,
        jobId.toString(),
        AssemblyEvents.TYPE_REQUESTED,
        new AssemblyEvents.AssemblyRequested(
            jobId, templateId, templateVersionNumber, requestedBy, Instant.now()));
    return job;
  }

  @Transactional
  public AssemblyJobEntity recordCompleted(UUID jobId, UUID resultDocumentId) {
    AssemblyJobEntity job = jobs.findById(jobId).orElseThrow();
    job.markCompleted(resultDocumentId);
    outbox.stage(
        AssemblyEvents.AGGREGATE_TYPE,
        jobId.toString(),
        AssemblyEvents.TYPE_COMPLETED,
        new AssemblyEvents.AssemblyCompleted(
            jobId, job.getTemplateId(), job.getTemplateVersionNumber(), Instant.now()));
    return job;
  }

  @Transactional
  public void recordFailed(UUID jobId, String code, String message) {
    AssemblyJobEntity job = jobs.findById(jobId).orElse(null);
    if (job == null) {
      // recordRequested itself failed before commit — nothing to update. Caller will rethrow.
      return;
    }
    job.markFailed(code, message);
    outbox.stage(
        AssemblyEvents.AGGREGATE_TYPE,
        jobId.toString(),
        AssemblyEvents.TYPE_FAILED,
        new AssemblyEvents.AssemblyFailed(jobId, code, message, Instant.now()));
  }
}
