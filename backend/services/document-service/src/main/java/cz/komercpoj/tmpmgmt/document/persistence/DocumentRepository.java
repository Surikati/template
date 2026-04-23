package cz.komercpoj.tmpmgmt.document.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<GeneratedDocumentEntity, UUID> {}
