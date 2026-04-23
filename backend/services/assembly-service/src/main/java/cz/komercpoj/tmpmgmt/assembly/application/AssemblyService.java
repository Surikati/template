package cz.komercpoj.tmpmgmt.assembly.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.komercpoj.tmpmgmt.assembly.application.events.AssemblyEvents;
import cz.komercpoj.tmpmgmt.assembly.client.DocumentServiceClient;
import cz.komercpoj.tmpmgmt.assembly.client.RenderingServiceClient;
import cz.komercpoj.tmpmgmt.assembly.client.TemplateServiceClient;
import cz.komercpoj.tmpmgmt.assembly.client.TemplateVersionDto;
import cz.komercpoj.tmpmgmt.assembly.persistence.AssemblyJobEntity;
import cz.komercpoj.tmpmgmt.assembly.persistence.AssemblyJobRepository;
import cz.komercpoj.tmpmgmt.common.DomainException;
import cz.komercpoj.tmpmgmt.common.NotFoundException;
import cz.komercpoj.tmpmgmt.outbox.OutboxWriter;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
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
 *   <li>Resolve any {@code clauseRef} nodes by fetching referenced clause versions.
 *   <li>Call rendering-service with the composed AST + input data.
 *   <li>Upload rendered bytes to document-service (which persists them in MinIO).
 *   <li>Update job to COMPLETED with resultDocumentId (or FAILED on error).
 *   <li>Emit outbox event so downstream (audit, notification) can react.
 * </ol>
 *
 * <p>Out of MVP scope: async job state machine with polling API, batch clause fetching
 * (currently one HTTP call per distinct clauseRef).
 */
@Service
public class AssemblyService {

    private static final Logger log = LoggerFactory.getLogger(AssemblyService.class);

    private final AssemblyJobRepository jobs;
    private final TemplateServiceClient templateClient;
    private final RenderingServiceClient renderingClient;
    private final DocumentServiceClient documentClient;
    private final ClauseResolver clauseResolver;
    private final OutboxWriter outbox;
    private final ObjectMapper mapper;

    public AssemblyService(
            AssemblyJobRepository jobs,
            TemplateServiceClient templateClient,
            RenderingServiceClient renderingClient,
            DocumentServiceClient documentClient,
            ClauseResolver clauseResolver,
            OutboxWriter outbox,
            ObjectMapper mapper) {
        this.jobs = jobs;
        this.templateClient = templateClient;
        this.renderingClient = renderingClient;
        this.documentClient = documentClient;
        this.clauseResolver = clauseResolver;
        this.outbox = outbox;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public AssemblyJobEntity getById(UUID id) {
        return jobs.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        "assembly.not_found", "Assembly job not found: " + id));
    }

    /**
     * Ephemeral preview — runs the same fetch + resolve + render pipeline as {@link #assemble}
     * but emits HTML and skips job persistence, outbox emission, and document upload.
     */
    public String preview(AssemblyCommand cmd) {
        TemplateVersionDto version = templateClient.getVersion(
                cmd.templateId(), cmd.templateVersionNumber());
        JsonNode composedAst = clauseResolver.resolveClauseRefs(version.content());

        var renderRequest = new RenderingServiceClient.RenderRequest(
                composedAst, cmd.data(), RenderingServiceClient.RenderFormat.HTML);
        var rendered = renderingClient.render(renderRequest);
        return new String(rendered.content(), java.nio.charset.StandardCharsets.UTF_8);
    }

    @Transactional
    public AssemblyResult assemble(AssemblyCommand cmd) {
        UUID jobId = UUID.randomUUID();
        JsonNode inputDataJson = mapper.valueToTree(cmd.data());
        AssemblyJobEntity job = AssemblyJobEntity.pending(
                jobId,
                cmd.templateId(),
                cmd.templateVersionNumber(),
                inputDataJson,
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

            job.markResolvingClauses();
            JsonNode composedAst = clauseResolver.resolveClauseRefs(version.content());

            job.markRendering();

            var renderRequest = new RenderingServiceClient.RenderRequest(
                    composedAst, cmd.data(), RenderingServiceClient.RenderFormat.DOCX);
            var rendered = renderingClient.render(renderRequest);

            // Hand off to document-service for durable storage in MinIO.
            var docRequest = new DocumentServiceClient.UploadDocumentRequest(
                    cmd.templateId(),
                    cmd.templateVersionNumber(),
                    jobId,
                    inputDataJson,
                    List.of(new DocumentServiceClient.FileInputDto(
                            DocumentServiceClient.FileFormat.DOCX,
                            Base64.getEncoder().encodeToString(rendered.content()))));
            var docResponse = documentClient.upload(docRequest);

            job.markCompleted(docResponse.id());

            outbox.stage(
                    AssemblyEvents.AGGREGATE_TYPE,
                    jobId.toString(),
                    AssemblyEvents.TYPE_COMPLETED,
                    new AssemblyEvents.AssemblyCompleted(
                            jobId, cmd.templateId(), cmd.templateVersionNumber(), Instant.now()));

            String downloadUrl = docResponse.files().isEmpty()
                    ? null : docResponse.files().get(0).downloadUrl();
            return new AssemblyResult(job, rendered.filename(), docResponse.id(), downloadUrl);
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

    public record AssemblyResult(
            AssemblyJobEntity job, String filename, UUID documentId, String downloadUrl) {}
}
