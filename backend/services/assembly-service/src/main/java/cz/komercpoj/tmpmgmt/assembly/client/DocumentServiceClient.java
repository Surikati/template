package cz.komercpoj.tmpmgmt.assembly.client;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "document-service", url = "${tmpmgmt.clients.document-service-url}")
public interface DocumentServiceClient {

    @PostMapping("/api/v1/documents")
    DocumentResponse upload(@RequestBody UploadDocumentRequest request);

    enum FileFormat { DOCX, PDF, HTML }

    record FileInputDto(FileFormat format, String contentBase64) {}

    record UploadDocumentRequest(
            UUID templateId,
            int templateVersionNumber,
            UUID assemblyJobId,
            JsonNode inputDataSnapshot,
            List<FileInputDto> files) {}

    record FileReferenceResponse(
            UUID id,
            FileFormat format,
            long sizeBytes,
            String sha256,
            String downloadUrl,
            Instant createdAt) {}

    record DocumentResponse(
            UUID id,
            UUID templateId,
            int templateVersionNumber,
            UUID assemblyJobId,
            UUID createdBy,
            Instant createdAt,
            List<FileReferenceResponse> files) {}
}
