package cz.komercpoj.tmpmgmt.document.api;

import cz.komercpoj.tmpmgmt.document.api.dto.DocumentResponse;
import cz.komercpoj.tmpmgmt.document.api.dto.FileInputDto;
import cz.komercpoj.tmpmgmt.document.api.dto.FileReferenceResponse;
import cz.komercpoj.tmpmgmt.document.api.dto.UploadDocumentRequest;
import cz.komercpoj.tmpmgmt.document.application.DocumentCommands;
import cz.komercpoj.tmpmgmt.document.application.DocumentService;
import cz.komercpoj.tmpmgmt.document.domain.FileFormat;
import cz.komercpoj.tmpmgmt.document.persistence.FileReferenceEntity;
import cz.komercpoj.tmpmgmt.document.persistence.GeneratedDocumentEntity;
import io.minio.GetObjectResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

  private final DocumentService service;

  public DocumentController(DocumentService service) {
    this.service = service;
  }

  @PostMapping
  public ResponseEntity<DocumentResponse> upload(
      @Valid @RequestBody UploadDocumentRequest req, @AuthenticationPrincipal Jwt jwt) {
    List<DocumentCommands.FileInput> fileInputs = req.files().stream().map(this::decode).toList();
    var doc =
        service.upload(
            new DocumentCommands.UploadDocument(
                req.templateId(),
                req.templateVersionNumber(),
                req.assemblyJobId(),
                req.inputDataSnapshot(),
                currentUserId(jwt),
                fileInputs));
    return ResponseEntity.created(URI.create("/api/v1/documents/" + doc.getId()))
        .body(toResponse(doc));
  }

  @GetMapping("/{id}")
  public DocumentResponse get(@PathVariable UUID id) {
    return toResponse(service.getById(id));
  }

  @GetMapping("/{id}/files/{format}")
  public ResponseEntity<InputStreamResource> downloadFile(
      @PathVariable UUID id, @PathVariable FileFormat format) {
    FileReferenceEntity ref = service.getFile(id, format);
    GetObjectResponse stream = service.download(ref);
    String filename = "document-" + id + "." + format.extension();
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
        .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(ref.getSizeBytes()))
        .contentType(MediaType.parseMediaType(format.mimeType()))
        .body(new InputStreamResource(stream));
  }

  private DocumentCommands.FileInput decode(FileInputDto dto) {
    return new DocumentCommands.FileInput(
        dto.format(), Base64.getDecoder().decode(dto.contentBase64()));
  }

  private DocumentResponse toResponse(GeneratedDocumentEntity doc) {
    List<FileReferenceResponse> files =
        doc.getFiles().stream()
            .map(
                f ->
                    new FileReferenceResponse(
                        f.getId(),
                        f.getFormat(),
                        f.getSizeBytes(),
                        f.getSha256(),
                        "/api/v1/documents/" + doc.getId() + "/files/" + f.getFormat(),
                        f.getCreatedAt()))
            .toList();
    return new DocumentResponse(
        doc.getId(),
        doc.getTemplateId(),
        doc.getTemplateVersionNumber(),
        doc.getAssemblyJobId(),
        doc.getCreatedBy(),
        doc.getCreatedAt(),
        files);
  }

  private UUID currentUserId(Jwt jwt) {
    return UUID.fromString(jwt.getSubject());
  }
}
