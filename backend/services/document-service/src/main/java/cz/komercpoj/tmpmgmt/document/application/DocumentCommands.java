package cz.komercpoj.tmpmgmt.document.application;

import com.fasterxml.jackson.databind.JsonNode;
import cz.komercpoj.tmpmgmt.document.domain.FileFormat;
import java.util.List;
import java.util.UUID;

public final class DocumentCommands {

  private DocumentCommands() {}

  public record FileInput(FileFormat format, byte[] content) {}

  public record UploadDocument(
      UUID templateId,
      int templateVersionNumber,
      UUID assemblyJobId,
      JsonNode inputDataSnapshot,
      UUID createdBy,
      List<FileInput> files) {}
}
