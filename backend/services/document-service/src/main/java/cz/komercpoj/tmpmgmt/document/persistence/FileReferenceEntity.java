package cz.komercpoj.tmpmgmt.document.persistence;

import cz.komercpoj.tmpmgmt.document.domain.FileFormat;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "file_reference")
@Getter
@Setter
@NoArgsConstructor
public class FileReferenceEntity {

  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "document_id")
  private GeneratedDocumentEntity document;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private FileFormat format;

  @Column(name = "minio_key", nullable = false, length = 500)
  private String minioKey;

  @Column(name = "size_bytes", nullable = false)
  private long sizeBytes;

  @Column(nullable = false, length = 64)
  private String sha256;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  public static FileReferenceEntity create(
      UUID id, FileFormat format, String minioKey, long sizeBytes, String sha256) {
    FileReferenceEntity f = new FileReferenceEntity();
    f.id = id;
    f.format = format;
    f.minioKey = minioKey;
    f.sizeBytes = sizeBytes;
    f.sha256 = sha256;
    f.createdAt = Instant.now();
    return f;
  }
}
