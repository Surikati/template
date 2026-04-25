package cz.komercpoj.tmpmgmt.assembly;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.*;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import cz.komercpoj.tmpmgmt.assembly.application.AssemblyService;
import cz.komercpoj.tmpmgmt.assembly.application.AssemblyService.AssemblyCommand;
import cz.komercpoj.tmpmgmt.assembly.domain.AssemblyState;
import cz.komercpoj.tmpmgmt.assembly.domain.OutputFormat;
import cz.komercpoj.tmpmgmt.assembly.persistence.AssemblyJobEntity;
import cz.komercpoj.tmpmgmt.assembly.persistence.AssemblyJobRepository;
import cz.komercpoj.tmpmgmt.common.DomainException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * End-to-end integration test for assembly-service.
 *
 * <ul>
 *   <li>Postgres + RabbitMQ via Testcontainers.
 *   <li>template-service, rendering-service, and document-service stubbed by WireMock on a single
 *       port (paths disambiguate).
 *   <li>Asserts DB persistence, outbox events, and Feign → HTTP wiring across 3 downstream
 *       services.
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("test")
@Import({TestcontainersConfig.class, TestSecurityConfig.class})
class AssemblyIT {

  private static final WireMockServer WIRE_MOCK =
      new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());

  static {
    // Start in a static initialiser so the port is known when @DynamicPropertySource runs,
    // i.e. before the Spring context bootstraps Feign clients.
    WIRE_MOCK.start();
  }

  @DynamicPropertySource
  static void overrideClientUrls(DynamicPropertyRegistry registry) {
    String base = "http://localhost:" + WIRE_MOCK.port();
    registry.add("tmpmgmt.clients.template-service-url", () -> base);
    registry.add("tmpmgmt.clients.clause-service-url", () -> base);
    registry.add("tmpmgmt.clients.rendering-service-url", () -> base);
    registry.add("tmpmgmt.clients.document-service-url", () -> base);
  }

  @AfterAll
  static void stopWireMock() {
    WIRE_MOCK.stop();
  }

  @BeforeEach
  void resetState() {
    WIRE_MOCK.resetAll();
    // Tests share the same Spring context (and therefore the same Postgres testcontainer).
    // Wipe job + outbox rows so each test sees a clean DB.
    jdbc.update("DELETE FROM assembly_job");
    jdbc.update("DELETE FROM outbox_event");
  }

  @Autowired AssemblyService service;
  @Autowired AssemblyJobRepository jobs;
  @Autowired JdbcTemplate jdbc;

  private final UUID actor = UUID.randomUUID();

  @Test
  void assemble_happyPath_uploadsToDocumentServiceAndCompletesJob() {
    UUID templateId = UUID.randomUUID();
    UUID expectedDocumentId = UUID.randomUUID();
    byte[] expectedBytes = "DOCX_BINARY".getBytes();

    WIRE_MOCK.stubFor(
        get(urlEqualTo("/api/v1/templates/" + templateId + "/versions/1"))
            .willReturn(okJson(templateVersionJson(templateId, 1))));
    WIRE_MOCK.stubFor(
        post(urlEqualTo("/api/v1/render")).willReturn(okJson(renderResponseJson(expectedBytes))));
    WIRE_MOCK.stubFor(
        post(urlEqualTo("/api/v1/documents"))
            .willReturn(
                okJson(
                    documentResponseJson(
                        expectedDocumentId, templateId, 1, expectedBytes.length))));

    var result =
        service.assemble(
            new AssemblyCommand(
                templateId,
                1,
                Map.of("client", Map.of("name", "ACME")),
                List.of(OutputFormat.DOCX),
                actor));

    assertThat(result.job().getState()).isEqualTo(AssemblyState.COMPLETED);
    assertThat(result.job().getCompletedAt()).isNotNull();
    assertThat(result.documentId()).isEqualTo(expectedDocumentId);
    assertThat(result.files()).hasSize(1);
    assertThat(result.files().get(0).format()).isEqualTo(OutputFormat.DOCX);
    assertThat(result.files().get(0).downloadUrl())
        .isEqualTo("/api/v1/documents/" + expectedDocumentId + "/files/DOCX");

    // Job row reflects the resulting document.
    AssemblyJobEntity saved = jobs.findById(result.job().getId()).orElseThrow();
    assertThat(saved.getResultDocumentId()).isEqualTo(expectedDocumentId);

    // Outbox events staged in order.
    List<Map<String, Object>> rows =
        jdbc.queryForList(
            "SELECT event_type FROM outbox_event WHERE aggregate_id = ? ORDER BY occurred_at",
            result.job().getId().toString());
    assertThat(rows).extracting(r -> r.get("event_type")).containsExactly("requested", "completed");

    // Every downstream service was hit exactly once.
    WIRE_MOCK.verify(
        1, getRequestedFor(urlEqualTo("/api/v1/templates/" + templateId + "/versions/1")));
    WIRE_MOCK.verify(1, postRequestedFor(urlEqualTo("/api/v1/render")));
    WIRE_MOCK.verify(1, postRequestedFor(urlEqualTo("/api/v1/documents")));
  }

  @Test
  void assemble_withClauseRef_resolvesAndIncludesClauseContent() {
    UUID templateId = UUID.randomUUID();
    UUID clauseId = UUID.randomUUID();
    UUID expectedDocumentId = UUID.randomUUID();
    byte[] expectedBytes = "DOCX_WITH_CLAUSE".getBytes();

    // Template references a clause version.
    WIRE_MOCK.stubFor(
        get(urlEqualTo("/api/v1/templates/" + templateId + "/versions/1"))
            .willReturn(okJson(templateVersionJsonWithClauseRef(templateId, 1, clauseId, 3))));

    // clause-service returns the clause fragment.
    WIRE_MOCK.stubFor(
        get(urlEqualTo("/api/v1/clauses/" + clauseId + "/versions/3"))
            .willReturn(okJson(clauseVersionJson(clauseId, 3))));

    WIRE_MOCK.stubFor(
        post(urlEqualTo("/api/v1/render")).willReturn(okJson(renderResponseJson(expectedBytes))));
    WIRE_MOCK.stubFor(
        post(urlEqualTo("/api/v1/documents"))
            .willReturn(
                okJson(
                    documentResponseJson(
                        expectedDocumentId, templateId, 1, expectedBytes.length))));

    var result =
        service.assemble(
            new AssemblyCommand(templateId, 1, Map.of(), List.of(OutputFormat.DOCX), actor));

    assertThat(result.job().getState()).isEqualTo(AssemblyState.COMPLETED);
    assertThat(result.documentId()).isEqualTo(expectedDocumentId);

    // clause-service WAS called to resolve the reference.
    WIRE_MOCK.verify(1, getRequestedFor(urlEqualTo("/api/v1/clauses/" + clauseId + "/versions/3")));

    // The request to rendering-service must NOT contain any clauseRef node any more —
    // the resolver inlined the clause content before hand-off.
    WIRE_MOCK.verify(
        postRequestedFor(urlEqualTo("/api/v1/render"))
            .withRequestBody(notMatching(".*clauseRef.*"))
            .withRequestBody(matching(".*GDPR doložka.*")));
  }

  @Test
  void assemble_clauseResolutionFails_marksJobFailed() {
    UUID templateId = UUID.randomUUID();
    UUID clauseId = UUID.randomUUID();

    WIRE_MOCK.stubFor(
        get(urlEqualTo("/api/v1/templates/" + templateId + "/versions/1"))
            .willReturn(okJson(templateVersionJsonWithClauseRef(templateId, 1, clauseId, 3))));
    WIRE_MOCK.stubFor(
        get(urlEqualTo("/api/v1/clauses/" + clauseId + "/versions/3"))
            .willReturn(aResponse().withStatus(404)));

    assertThatThrownBy(
            () ->
                service.assemble(
                    new AssemblyCommand(
                        templateId, 1, Map.of(), List.of(OutputFormat.DOCX), actor)))
        .isInstanceOf(DomainException.class);

    AssemblyJobEntity job = jobs.findAll().get(0);
    assertThat(job.getState()).isEqualTo(AssemblyState.FAILED);

    WIRE_MOCK.verify(0, postRequestedFor(urlEqualTo("/api/v1/render")));
    WIRE_MOCK.verify(0, postRequestedFor(urlEqualTo("/api/v1/documents")));
  }

  @Test
  void assemble_renderingFails_marksJobFailedAndDoesNotCallDocumentService() {
    UUID templateId = UUID.randomUUID();

    WIRE_MOCK.stubFor(
        get(urlEqualTo("/api/v1/templates/" + templateId + "/versions/1"))
            .willReturn(okJson(templateVersionJson(templateId, 1))));
    WIRE_MOCK.stubFor(
        post(urlEqualTo("/api/v1/render"))
            .willReturn(aResponse().withStatus(500).withBody("{\"detail\":\"boom\"}")));

    assertThatThrownBy(
            () ->
                service.assemble(
                    new AssemblyCommand(
                        templateId, 1, Map.of(), List.of(OutputFormat.DOCX), actor)))
        .isInstanceOf(DomainException.class);

    AssemblyJobEntity job = jobs.findAll().get(0);
    assertThat(job.getState()).isEqualTo(AssemblyState.FAILED);
    assertThat(job.getErrorMessage()).isNotBlank();
    assertThat(job.getResultDocumentId()).isNull();

    List<Map<String, Object>> rows =
        jdbc.queryForList(
            "SELECT event_type FROM outbox_event WHERE aggregate_id = ? ORDER BY occurred_at",
            job.getId().toString());
    assertThat(rows).extracting(r -> r.get("event_type")).containsExactly("requested", "failed");

    // document-service never got called when rendering blew up
    WIRE_MOCK.verify(0, postRequestedFor(urlEqualTo("/api/v1/documents")));
  }

  @Test
  void assemble_documentUploadFails_marksJobFailed() {
    UUID templateId = UUID.randomUUID();
    byte[] expectedBytes = "DOCX_BINARY".getBytes();

    WIRE_MOCK.stubFor(
        get(urlEqualTo("/api/v1/templates/" + templateId + "/versions/1"))
            .willReturn(okJson(templateVersionJson(templateId, 1))));
    WIRE_MOCK.stubFor(
        post(urlEqualTo("/api/v1/render")).willReturn(okJson(renderResponseJson(expectedBytes))));
    WIRE_MOCK.stubFor(
        post(urlEqualTo("/api/v1/documents"))
            .willReturn(aResponse().withStatus(503).withBody("{\"detail\":\"MinIO down\"}")));

    assertThatThrownBy(
            () ->
                service.assemble(
                    new AssemblyCommand(
                        templateId, 1, Map.of(), List.of(OutputFormat.DOCX), actor)))
        .isInstanceOf(DomainException.class);

    AssemblyJobEntity job = jobs.findAll().get(0);
    assertThat(job.getState()).isEqualTo(AssemblyState.FAILED);

    List<Map<String, Object>> rows =
        jdbc.queryForList(
            "SELECT event_type FROM outbox_event WHERE aggregate_id = ? ORDER BY occurred_at",
            job.getId().toString());
    assertThat(rows).extracting(r -> r.get("event_type")).containsExactly("requested", "failed");
  }

  @Test
  void assemble_templateVersionNotFound_marksJobFailed() {
    UUID templateId = UUID.randomUUID();

    WIRE_MOCK.stubFor(
        get(urlEqualTo("/api/v1/templates/" + templateId + "/versions/99"))
            .willReturn(aResponse().withStatus(404)));

    assertThatThrownBy(
            () ->
                service.assemble(
                    new AssemblyCommand(
                        templateId, 99, Map.of(), List.of(OutputFormat.DOCX), actor)))
        .isInstanceOf(DomainException.class);

    AssemblyJobEntity job = jobs.findAll().get(0);
    assertThat(job.getState()).isEqualTo(AssemblyState.FAILED);

    WIRE_MOCK.verify(0, postRequestedFor(urlEqualTo("/api/v1/render")));
    WIRE_MOCK.verify(0, postRequestedFor(urlEqualTo("/api/v1/documents")));
  }

  // -- helpers --------------------------------------------------------------------------

  private static String templateVersionJson(UUID templateId, int versionNumber) {
    return """
                {
                  "id": "%s",
                  "templateId": "%s",
                  "versionNumber": %d,
                  "content": {
                    "type": "doc",
                    "content": [
                      { "type": "paragraph", "content": [
                        { "type": "text", "text": "Ahoj " },
                        { "type": "variable", "attrs": { "path": "client.name", "dataType": "STRING" } }
                      ]}
                    ]
                  },
                  "variablesSchema": { "type": "object" },
                  "changeNote": "initial",
                  "publishedAt": "2026-04-23T10:00:00Z",
                  "publishedBy": "%s"
                }
                """
        .formatted(UUID.randomUUID(), templateId, versionNumber, UUID.randomUUID());
  }

  private static String templateVersionJsonWithClauseRef(
      UUID templateId, int versionNumber, UUID clauseId, int clauseVersion) {
    return """
                {
                  "id": "%s",
                  "templateId": "%s",
                  "versionNumber": %d,
                  "content": {
                    "type": "doc",
                    "content": [
                      { "type": "paragraph", "content": [{ "type": "text", "text": "Úvod." }] },
                      { "type": "clauseRef", "attrs": { "clauseId": "%s", "versionNumber": %d } },
                      { "type": "paragraph", "content": [{ "type": "text", "text": "Závěr." }] }
                    ]
                  },
                  "variablesSchema": { "type": "object" },
                  "changeNote": "with clause",
                  "publishedAt": "2026-04-23T10:00:00Z",
                  "publishedBy": "%s"
                }
                """
        .formatted(
            UUID.randomUUID(),
            templateId,
            versionNumber,
            clauseId,
            clauseVersion,
            UUID.randomUUID());
  }

  private static String clauseVersionJson(UUID clauseId, int versionNumber) {
    return """
                {
                  "id": "%s",
                  "clauseId": "%s",
                  "versionNumber": %d,
                  "content": {
                    "type": "fragment",
                    "content": [
                      { "type": "paragraph", "content": [
                        { "type": "text", "text": "GDPR doložka podle nařízení 2016/679." }
                      ]}
                    ]
                  },
                  "changeNote": "initial",
                  "publishedAt": "2026-04-23T10:00:00Z",
                  "publishedBy": "%s"
                }
                """
        .formatted(UUID.randomUUID(), clauseId, versionNumber, UUID.randomUUID());
  }

  private static String renderResponseJson(byte[] content) {
    String base64 = Base64.getEncoder().encodeToString(content);
    return """
                {
                  "format": "DOCX",
                  "filename": "document.docx",
                  "content": "%s"
                }
                """
        .formatted(base64);
  }

  private static String documentResponseJson(UUID docId, UUID templateId, int version, int size) {
    UUID fileId = UUID.randomUUID();
    return """
                {
                  "id": "%s",
                  "templateId": "%s",
                  "templateVersionNumber": %d,
                  "assemblyJobId": "%s",
                  "createdBy": "%s",
                  "createdAt": "2026-04-23T10:00:00Z",
                  "files": [
                    {
                      "id": "%s",
                      "format": "DOCX",
                      "sizeBytes": %d,
                      "sha256": "abc123",
                      "downloadUrl": "/api/v1/documents/%s/files/DOCX",
                      "createdAt": "2026-04-23T10:00:00Z"
                    }
                  ]
                }
                """
        .formatted(
            docId, templateId, version, UUID.randomUUID(), UUID.randomUUID(), fileId, size, docId);
  }
}
