package cz.komercpoj.tmpmgmt.assembly.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.komercpoj.tmpmgmt.assembly.application.events.AssemblyEvents;
import cz.komercpoj.tmpmgmt.assembly.client.RenderingServiceClient;
import cz.komercpoj.tmpmgmt.assembly.client.TemplateServiceClient;
import cz.komercpoj.tmpmgmt.assembly.client.TemplateVersionDto;
import cz.komercpoj.tmpmgmt.assembly.persistence.AssemblyJobEntity;
import cz.komercpoj.tmpmgmt.assembly.persistence.AssemblyJobRepository;
import cz.komercpoj.tmpmgmt.common.DomainException;
import cz.komercpoj.tmpmgmt.common.NotFoundException;
import cz.komercpoj.tmpmgmt.outbox.OutboxWriter;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates document assembly. MVP is synchronous:
 *
 * <ol>
 *   <li>Persist a job row (state=PENDING).
 *   <li>Fetch the immutable template version from template-service.
 *   <li>Call rendering-service with the template content + input data.
 *   <li>Update job to COMPLETED (or FAILED on error).
 *   <li>Emit outbox event so downstream (document-service, audit) can react.
 * </ol>
 *
 * <p>Out of MVP scope: clauseRef resolution (TODO — requires clause-service calls), document
 * upload to MinIO via document-service (currently bytes are returned inline), async job state
 * machine with polling API.
 */
@Service
public class AssemblyService {

    private static final Logger log = LoggerFactory.getLogger(AssemblyService.class);

    private final AssemblyJobRepository jobs;
    private final TemplateServiceClient templateClient;
    private final RenderingServiceClient renderingClient;
    private final OutboxWriter outbox;
    private final ObjectMapper mapper;

    public AssemblyService(
            AssemblyJobRepository jobs,
            TemplateServiceClient templateClient,
            RenderingServiceClient renderingClient,
            OutboxWriter outbox,
            ObjectMapper mapper) {
        this.jobs = jobs;
        this.templateClient = templateClient;
        this.renderingClient = renderingClient;
        this.outbox = outbox;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public AssemblyJobEntity getById(UUID id) {
        return jobs.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        "assembly.not_found", "Assembly job not found: " + id));
    }

    @Transactional
    public AssemblyResult assemble(AssemblyCommand cmd) {
        UUID jobId = UUID.randomUUID();
        AssemblyJobEntity job = AssemblyJobEntity.pending(
                jobId,
                cmd.templateId(),
                cmd.templateVersionNumber(),
                mapper.valueToTree(cmd.data()),
                new String[] {cmd.format().name()},
                cmd.requestedBy());
        jobs.save(job);

        outbox.stage(
                AssemblyEvents.AGGREGATE_TYPE,
                jobId.toString(),
                AssemblyEvents.TYPE_REQUESTED,
                new AssemblyEvents.AssemblyRequested(
                        jobId, cmd.templateId(), cmd.templateVersionNumber(),
                        cmd.requestedBy(), Instant.now()));

        try {
            TemplateVersionDto version = templateClient.getVersion(
                    cmd.templateId(), cmd.templateVersionNumber());

            job.markRendering();

            var renderRequest = new RenderingServiceClient.RenderRequest(
                    version.content(), cmd.data(), RenderingServiceClient.RenderFormat.DOCX);
            var rendered = renderingClient.render(renderRequest);

            // TODO: upload rendered.content() to MinIO via document-service; store its id as resultDocumentId.
            job.markCompleted(null);

            outbox.stage(
                    AssemblyEvents.AGGREGATE_TYPE,
                    jobId.toString(),
                    AssemblyEvents.TYPE_COMPLETED,
                    new AssemblyEvents.AssemblyCompleted(
                            jobId, cmd.templateId(), cmd.templateVersionNumber(), Instant.now()));

            return new AssemblyResult(job, rendered.filename(), rendered.content());
        } catch (Exception e) {
            log.warn("Assembly job {} failed: {}", jobId, e.getMessage());
            String code = (e instanceof DomainException de) ? de.getErrorCode() : "assembly.error";
            job.markFailed(code, e.getMessage());
            outbox.stage(
                    AssemblyEvents.AGGREGATE_TYPE,
                    jobId.toString(),
                    AssemblyEvents.TYPE_FAILED,
                    new AssemblyEvents.AssemblyFailed(jobId, code, e.getMessage(), Instant.now()));
            throw new DomainException(code, "Assembly failed: " + e.getMessage(), e);
        }
    }

    public record AssemblyCommand(
            UUID templateId,
            int templateVersionNumber,
            Map<String, Object> data,
            Format format,
            UUID requestedBy) {
        public enum Format { DOCX }
    }

    public record AssemblyResult(AssemblyJobEntity job, String filename, byte[] content) {}
}
