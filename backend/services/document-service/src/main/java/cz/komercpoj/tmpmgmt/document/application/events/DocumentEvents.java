package cz.komercpoj.tmpmgmt.document.application.events;

import cz.komercpoj.tmpmgmt.document.domain.FileFormat;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class DocumentEvents {

  private DocumentEvents() {}

  public static final String AGGREGATE_TYPE = "document";
  public static final String TYPE_GENERATED = "generated";

  public record DocumentGenerated(
      UUID documentId,
      UUID templateId,
      int templateVersionNumber,
      UUID assemblyJobId,
      List<FileRef> files,
      UUID createdBy,
      Instant occurredAt) {

    public record FileRef(FileFormat format, long sizeBytes, String sha256) {}
  }
}
