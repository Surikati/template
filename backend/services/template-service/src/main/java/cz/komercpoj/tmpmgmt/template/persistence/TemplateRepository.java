package cz.komercpoj.tmpmgmt.template.persistence;

import cz.komercpoj.tmpmgmt.template.domain.TemplateStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TemplateRepository extends JpaRepository<TemplateEntity, UUID> {

    Optional<TemplateEntity> findBySlug(String slug);

    boolean existsBySlug(String slug);

    Page<TemplateEntity> findByStatus(TemplateStatus status, Pageable pageable);

    Page<TemplateEntity> findByStatusAndCategory(TemplateStatus status, String category, Pageable pageable);
}
