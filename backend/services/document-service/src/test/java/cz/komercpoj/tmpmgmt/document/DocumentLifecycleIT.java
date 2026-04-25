package cz.komercpoj.tmpmgmt.document;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.komercpoj.tmpmgmt.document.application.DocumentCommands;
import cz.komercpoj.tmpmgmt.document.application.DocumentService;
import cz.komercpoj.tmpmgmt.document.domain.FileFormat;
import cz.komercpoj.tmpmgmt.document.persistence.DocumentRepository;
import cz.komercpoj.tmpmgmt.document.persistence.FileReferenceEntity;
import io.minio.GetObjectResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * Full upload → MinIO → download path. Uses a GenericContainer for MinIO (there's no official
 * Testcontainers module for it).
 */
@SpringBootTest
@ActiveProfiles("test")
@Import({TestcontainersConfig.class, TestSecurityConfig.class})
class DocumentLifecycleIT {

  @SuppressWarnings("resource")
  private static final GenericContainer<?> MINIO =
      new GenericContainer<>(DockerImageName.parse("minio/minio:RELEASE.2024-10-13T13-34-11Z"))
          .withExposedPorts(9000)
          .withEnv("MINIO_ROOT_USER", "minioadmin")
          .withEnv("MINIO_ROOT_PASSWORD", "minioadmin")
          .withCommand("server", "/data")
          .waitingFor(Wait.forListeningPort());

  @BeforeAll
  static void startMinio() {
    MINIO.start();
  }

  @AfterAll
  static void stopMinio() {
    MINIO.stop();
  }

  @DynamicPropertySource
  static void minioProps(DynamicPropertyRegistry registry) {
    registry.add(
        "tmpmgmt.minio.endpoint",
        () -> "http://" + MINIO.getHost() + ":" + MINIO.getMappedPort(9000));
    registry.add("tmpmgmt.minio.access-key", () -> "minioadmin");
    registry.add("tmpmgmt.minio.secret-key", () -> "minioadmin");
    registry.add("tmpmgmt.minio.bucket", () -> "tmpmgmt-documents-test");
  }

  @Autowired DocumentService service;
  @Autowired DocumentRepository docs;
  @Autowired JdbcTemplate jdbc;
  @Autowired ObjectMapper mapper;

  @Test
  void uploadsToMinio_persistsMetadata_emitsEvent_andStreamsBack() throws Exception {
    byte[] docxBytes = "FAKE_DOCX_BINARY_CONTENT".getBytes();
    UUID templateId = UUID.randomUUID();
    UUID jobId = UUID.randomUUID();
    UUID createdBy = UUID.randomUUID();

    var result =
        service.upload(
            new DocumentCommands.UploadDocument(
                templateId,
                1,
                jobId,
                mapper.valueToTree(Map.of("client", Map.of("name", "ACME"))),
                createdBy,
                List.of(new DocumentCommands.FileInput(FileFormat.DOCX, docxBytes))));

    // Metadata persisted
    assertThat(docs.findById(result.getId())).isPresent();
    assertThat(result.getFiles()).hasSize(1);
    FileReferenceEntity fileRef = result.getFiles().get(0);
    assertThat(fileRef.getSizeBytes()).isEqualTo(docxBytes.length);
    assertThat(fileRef.getSha256()).hasSize(64); // hex-encoded SHA-256 is 64 chars
    assertThat(fileRef.getMinioKey()).startsWith("generated/").endsWith(".docx");

    // Outbox event
    List<Map<String, Object>> events =
        jdbc.queryForList(
            "SELECT event_type FROM outbox_event WHERE aggregate_id = ?",
            result.getId().toString());
    assertThat(events).extracting(r -> r.get("event_type")).containsExactly("generated");

    // Actual bytes retrievable from MinIO
    try (GetObjectResponse stream = service.download(fileRef)) {
      byte[] downloaded = stream.readAllBytes();
      assertThat(downloaded).isEqualTo(docxBytes);
    }
  }

  @Test
  void uploadMultipleFormats_storesEachUnderDistinctKey() {
    byte[] docx = "DOCX_CONTENT".getBytes();
    byte[] pdf = "PDF_CONTENT".getBytes();

    var result =
        service.upload(
            new DocumentCommands.UploadDocument(
                UUID.randomUUID(),
                1,
                UUID.randomUUID(),
                mapper.createObjectNode(),
                UUID.randomUUID(),
                List.of(
                    new DocumentCommands.FileInput(FileFormat.DOCX, docx),
                    new DocumentCommands.FileInput(FileFormat.PDF, pdf))));

    assertThat(result.getFiles()).hasSize(2);
    var keys = result.getFiles().stream().map(FileReferenceEntity::getMinioKey).toList();
    assertThat(keys).hasSize(2).doesNotHaveDuplicates();
  }
}
