package cz.komercpoj.tmpmgmt.assembly.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import cz.komercpoj.tmpmgmt.assembly.domain.AssemblyState;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "assembly_job")
@Getter
@Setter
@NoArgsConstructor
public class AssemblyJobEntity {

    @Id
    private UUID id;

    @Column(name = "template_id", nullable = false)
    private UUID templateId;

    @Column(name = "template_version_number", nullable = false)
    private int templateVersionNumber;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "input_data", nullable = false, columnDefinition = "jsonb")
    private JsonNode inputData;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "requested_formats", columnDefinition = "text[]", nullable = false)
    private String[] requestedFormats;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AssemblyState state;

    @Column(name = "result_document_id")
    private UUID resultDocumentId;

    @Column(name = "error_code", length = 100)
    private String errorCode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "requested_by", nullable = false)
    private UUID requestedBy;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    public static AssemblyJobEntity pending(
            UUID id,
            UUID templateId,
            int versionNumber,
            JsonNode inputData,
            String[] formats,
            UUID requestedBy) {
        AssemblyJobEntity j = new AssemblyJobEntity();
        j.id = id;
        j.templateId = templateId;
        j.templateVersionNumber = versionNumber;
        j.inputData = inputData;
        j.requestedFormats = formats;
        j.state = AssemblyState.PENDING;
        j.requestedBy = requestedBy;
        j.requestedAt = Instant.now();
        return j;
    }

    public void markResolvingClauses() {
        this.state = AssemblyState.RESOLVING_CLAUSES;
    }

    public void markRendering() {
        this.state = AssemblyState.RENDERING;
    }

    public void markCompleted(UUID resultDocumentId) {
        this.state = AssemblyState.COMPLETED;
        this.resultDocumentId = resultDocumentId;
        this.completedAt = Instant.now();
    }

    public void markFailed(String code, String message) {
        this.state = AssemblyState.FAILED;
        this.errorCode = code;
        this.errorMessage = message;
        this.completedAt = Instant.now();
    }
}
