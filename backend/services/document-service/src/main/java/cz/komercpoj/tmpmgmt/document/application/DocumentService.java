package cz.komercpoj.tmpmgmt.document.application;

import cz.komercpoj.tmpmgmt.common.DomainException;
import cz.komercpoj.tmpmgmt.common.NotFoundException;
import cz.komercpoj.tmpmgmt.document.application.DocumentCommands.FileInput;
import cz.komercpoj.tmpmgmt.document.application.DocumentCommands.UploadDocument;
import cz.komercpoj.tmpmgmt.document.application.events.DocumentEvents;
import cz.komercpoj.tmpmgmt.document.config.MinioProperties;
import cz.komercpoj.tmpmgmt.document.domain.FileFormat;
import cz.komercpoj.tmpmgmt.document.persistence.DocumentRepository;
import cz.komercpoj.tmpmgmt.document.persistence.FileReferenceEntity;
import cz.komercpoj.tmpmgmt.document.persistence.GeneratedDocumentEntity;
import cz.komercpoj.tmpmgmt.outbox.OutboxWriter;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DocumentService {

  private final DocumentRepository docs;
  private final MinioClient minio;
  private final MinioProperties minioProps;
  private final OutboxWriter outbox;

  public DocumentService(
      DocumentRepository docs, MinioClient minio, MinioProperties minioProps, OutboxWriter outbox) {
    this.docs = docs;
    this.minio = minio;
    this.minioProps = minioProps;
    this.outbox = outbox;
  }

  @Transactional
  public GeneratedDocumentEntity upload(UploadDocument cmd) {
    if (cmd.files() == null || cmd.files().isEmpty()) {
      throw new DomainException("document.no_files", "Upload must include at least one file.");
    }
    UUID id = UUID.randomUUID();
    GeneratedDocumentEntity doc =
        GeneratedDocumentEntity.create(
            id,
            cmd.templateId(),
            cmd.templateVersionNumber(),
            cmd.assemblyJobId(),
            cmd.inputDataSnapshot(),
            cmd.createdBy());

    List<DocumentEvents.DocumentGenerated.FileRef> eventFiles = new ArrayList<>();
    for (FileInput fi : cmd.files()) {
      String key = minioKey(id, fi.format());
      String sha = sha256(fi.content());
      putToMinio(key, fi.content(), fi.format());

      FileReferenceEntity fr =
          FileReferenceEntity.create(UUID.randomUUID(), fi.format(), key, fi.content().length, sha);
      doc.addFile(fr);

      eventFiles.add(
          new DocumentEvents.DocumentGenerated.FileRef(fi.format(), fi.content().length, sha));
    }

    docs.save(doc);

    outbox.stage(
        DocumentEvents.AGGREGATE_TYPE,
        id.toString(),
        DocumentEvents.TYPE_GENERATED,
        new DocumentEvents.DocumentGenerated(
            id,
            cmd.templateId(),
            cmd.templateVersionNumber(),
            cmd.assemblyJobId(),
            eventFiles,
            cmd.createdBy(),
            Instant.now()));
    return doc;
  }

  @Transactional(readOnly = true)
  public GeneratedDocumentEntity getById(UUID id) {
    return docs.findById(id)
        .orElseThrow(
            () -> new NotFoundException("document.not_found", "Document not found: " + id));
  }

  @Transactional(readOnly = true)
  public FileReferenceEntity getFile(UUID documentId, FileFormat format) {
    GeneratedDocumentEntity doc = getById(documentId);
    return doc.getFiles().stream()
        .filter(f -> f.getFormat() == format)
        .findFirst()
        .orElseThrow(
            () ->
                new NotFoundException(
                    "document.file_not_found",
                    "Document " + documentId + " has no " + format + " file."));
  }

  /** Streams the binary from MinIO. Caller must close the returned stream. */
  public GetObjectResponse download(FileReferenceEntity ref) {
    try {
      return minio.getObject(
          GetObjectArgs.builder().bucket(minioProps.bucket()).object(ref.getMinioKey()).build());
    } catch (Exception e) {
      throw new DomainException(
          "document.minio_read_failed",
          "Failed to read " + ref.getMinioKey() + ": " + e.getMessage(),
          e);
    }
  }

  private void putToMinio(String key, byte[] content, FileFormat format) {
    try {
      minio.putObject(
          PutObjectArgs.builder().bucket(minioProps.bucket()).object(key).stream(
                  new ByteArrayInputStream(content), content.length, -1)
              .contentType(format.mimeType())
              .build());
    } catch (Exception e) {
      throw new DomainException(
          "document.minio_write_failed", "Failed to store " + key + ": " + e.getMessage(), e);
    }
  }

  private String minioKey(UUID docId, FileFormat format) {
    return "generated/" + docId + "/" + format.name().toLowerCase() + "." + format.extension();
  }

  private static String sha256(byte[] bytes) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(md.digest(bytes));
    } catch (Exception e) {
      throw new DomainException("document.hash_failed", e.getMessage(), e);
    }
  }
}
