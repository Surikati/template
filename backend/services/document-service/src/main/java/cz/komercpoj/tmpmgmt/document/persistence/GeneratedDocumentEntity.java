package cz.komercpoj.tmpmgmt.document.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "generated_document")
@Getter
@Setter
@NoArgsConstructor
public class GeneratedDocumentEntity {

    @Id
    private UUID id;

    @Column(name = "template_id", nullable = false)
    private UUID templateId;

    @Column(name = "template_version_number", nullable = false)
    private int templateVersionNumber;

    @Column(name = "assembly_job_id", nullable = false)
    private UUID assemblyJobId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "input_data_snapshot", nullable = false, columnDefinition = "jsonb")
    private JsonNode inputDataSnapshot;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(
            mappedBy = "document",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER)
    private List<FileReferenceEntity> files = new ArrayList<>();

    public static GeneratedDocumentEntity create(
            UUID id,
            UUID templateId,
            int templateVersionNumber,
            UUID assemblyJobId,
            JsonNode inputDataSnapshot,
            UUID createdBy) {
        GeneratedDocumentEntity d = new GeneratedDocumentEntity();
        d.id = id;
        d.templateId = templateId;
        d.templateVersionNumber = templateVersionNumber;
        d.assemblyJobId = assemblyJobId;
        d.inputDataSnapshot = inputDataSnapshot;
        d.createdBy = createdBy;
        d.createdAt = Instant.now();
        return d;
    }

    public void addFile(FileReferenceEntity f) {
        f.setDocument(this);
        this.files.add(f);
    }
}
