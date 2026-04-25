package cz.komercpoj.tmpmgmt.assembly.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.komercpoj.tmpmgmt.assembly.client.DocumentServiceClient;
import cz.komercpoj.tmpmgmt.assembly.client.RenderingServiceClient;
import cz.komercpoj.tmpmgmt.assembly.client.TemplateServiceClient;
import cz.komercpoj.tmpmgmt.assembly.client.TemplateVersionDto;
import cz.komercpoj.tmpmgmt.assembly.domain.OutputFormat;
import cz.komercpoj.tmpmgmt.assembly.persistence.AssemblyJobEntity;
import cz.komercpoj.tmpmgmt.assembly.persistence.AssemblyJobRepository;
import cz.komercpoj.tmpmgmt.common.DomainException;
import cz.komercpoj.tmpmgmt.common.NotFoundException;
import java.util.ArrayList;
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
 *   <li>Persist a job row (state=PENDING) — committed immediately.
 *   <li>Fetch the immutable template version from template-service.
 *   <li>Resolve any {@code clauseRef} nodes by fetching referenced clause versions.
 *   <li>For each requested format, call rendering-service with the composed AST + input data.
 *   <li>Upload all rendered bytes in one document-service call (single document, multi-file).
 *   <li>Update job to COMPLETED with resultDocumentId (or FAILED on error) — committed in its own
 *       transaction so the FAILED state survives the bubbled-up exception.
 *   <li>Emit outbox event so downstream (audit, notification) can react.
 * </ol>
 *
 * <p>Each lifecycle write goes through {@link JobLifecycleRecorder} so it commits independently —
 * the orchestrator itself is not {@code @Transactional}, otherwise a downstream Feign error would
 * roll back the whole thing and leave no audit trail.
 *
 * <p>Out of MVP scope: async job state machine with polling API, batch clause fetching (currently
 * one HTTP call per distinct clauseRef).
 */
@Service
public class AssemblyService {

  private static final Logger log = LoggerFactory.getLogger(AssemblyService.class);

  private final AssemblyJobRepository jobs;
  private final TemplateServiceClient templateClient;
  private final RenderingServiceClient renderingClient;
  private final DocumentServiceClient documentClient;
  private final ClauseResolver clauseResolver;
  private final JobLifecycleRecorder lifecycle;
  private final ObjectMapper mapper;

  public AssemblyService(
      AssemblyJobRepository jobs,
      TemplateServiceClient templateClient,
      RenderingServiceClient renderingClient,
      DocumentServiceClient documentClient,
      ClauseResolver clauseResolver,
      JobLifecycleRecorder lifecycle,
      ObjectMapper mapper) {
    this.jobs = jobs;
    this.templateClient = templateClient;
    this.renderingClient = renderingClient;
    this.documentClient = documentClient;
    this.clauseResolver = clauseResolver;
    this.lifecycle = lifecycle;
    this.mapper = mapper;
  }

  @Transactional(readOnly = true)
  public AssemblyJobEntity getById(UUID id) {
    return jobs.findById(id)
        .orElseThrow(
            () -> new NotFoundException("assembly.not_found", "Assembly job not found: " + id));
  }

  /**
   * Ephemeral preview — runs the same fetch + resolve + render pipeline as {@link #assemble} but
   * emits HTML and skips job persistence, outbox emission, and document upload.
   */
  public String preview(AssemblyCommand cmd) {
    TemplateVersionDto version =
        templateClient.getVersion(cmd.templateId(), cmd.templateVersionNumber());
    JsonNode composedAst = clauseResolver.resolveClauseRefs(version.content());

    var renderRequest =
        new RenderingServiceClient.RenderRequest(
            composedAst, cmd.data(), RenderingServiceClient.RenderFormat.HTML);
    var rendered = renderingClient.render(renderRequest);
    return new String(rendered.content(), java.nio.charset.StandardCharsets.UTF_8);
  }

  public AssemblyResult assemble(AssemblyCommand cmd) {
    UUID jobId = UUID.randomUUID();
    JsonNode inputDataJson = mapper.valueToTree(cmd.data());
    String[] formatNames = cmd.formats().stream().map(Enum::name).toArray(String[]::new);

    AssemblyJobEntity job =
        lifecycle.recordRequested(
            jobId,
            cmd.templateId(),
            cmd.templateVersionNumber(),
            inputDataJson,
            formatNames,
            cmd.requestedBy());

    try {
      TemplateVersionDto version =
          templateClient.getVersion(cmd.templateId(), cmd.templateVersionNumber());

      JsonNode composedAst = clauseResolver.resolveClauseRefs(version.content());

      // Render each requested format. AST and clause resolution are shared across formats —
      // only the rendering call duplicates per format.
      List<RenderedFormat> rendered = new ArrayList<>(cmd.formats().size());
      for (OutputFormat fmt : cmd.formats()) {
        var resp =
            renderingClient.render(
                new RenderingServiceClient.RenderRequest(
                    composedAst, cmd.data(), toRenderFormat(fmt)));
        rendered.add(new RenderedFormat(fmt, resp.filename(), resp.content()));
      }

      // Upload all formats as one document. document-service stores them as separate
      // FileReference rows under a shared GeneratedDocument id.
      List<DocumentServiceClient.FileInputDto> fileInputs =
          rendered.stream()
              .map(
                  r ->
                      new DocumentServiceClient.FileInputDto(
                          toFileFormat(r.format()), Base64.getEncoder().encodeToString(r.bytes())))
              .toList();
      var docResponse =
          documentClient.upload(
              new DocumentServiceClient.UploadDocumentRequest(
                  cmd.templateId(), cmd.templateVersionNumber(), jobId, inputDataJson, fileInputs));

      AssemblyJobEntity completed = lifecycle.recordCompleted(jobId, docResponse.id());

      // Pair each rendered format with its document-service file reference (matched by format).
      // document-service is expected to preserve order, but the match-by-format guard here
      // tolerates any reordering.
      List<AssembledFile> outputs = new ArrayList<>(rendered.size());
      for (RenderedFormat r : rendered) {
        var ref =
            docResponse.files().stream()
                .filter(f -> f.format() == toFileFormat(r.format()))
                .findFirst()
                .orElse(null);
        outputs.add(
            new AssembledFile(r.format(), r.filename(), ref == null ? null : ref.downloadUrl()));
      }
      return new AssemblyResult(completed, docResponse.id(), outputs);
    } catch (Exception e) {
      log.warn("Assembly job {} failed: {}", jobId, e.getMessage());
      String code = (e instanceof DomainException de) ? de.getErrorCode() : "assembly.error";
      lifecycle.recordFailed(jobId, code, e.getMessage());
      throw new DomainException(code, "Assembly failed: " + e.getMessage(), e);
    }
  }

  public record AssemblyCommand(
      UUID templateId,
      int templateVersionNumber,
      Map<String, Object> data,
      List<OutputFormat> formats,
      UUID requestedBy) {}

  public record AssemblyResult(AssemblyJobEntity job, UUID documentId, List<AssembledFile> files) {}

  public record AssembledFile(OutputFormat format, String filename, String downloadUrl) {}

  private record RenderedFormat(OutputFormat format, String filename, byte[] bytes) {}

  private static RenderingServiceClient.RenderFormat toRenderFormat(OutputFormat f) {
    return switch (f) {
      case DOCX -> RenderingServiceClient.RenderFormat.DOCX;
      case PDF -> RenderingServiceClient.RenderFormat.PDF;
    };
  }

  private static DocumentServiceClient.FileFormat toFileFormat(OutputFormat f) {
    return switch (f) {
      case DOCX -> DocumentServiceClient.FileFormat.DOCX;
      case PDF -> DocumentServiceClient.FileFormat.PDF;
    };
  }
}
