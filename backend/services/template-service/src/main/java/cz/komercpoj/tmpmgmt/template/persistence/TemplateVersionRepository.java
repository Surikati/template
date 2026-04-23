package cz.komercpoj.tmpmgmt.template.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TemplateVersionRepository extends JpaRepository<TemplateVersionEntity, UUID> {

    List<TemplateVersionEntity> findByTemplateIdOrderByVersionNumberDesc(UUID templateId);

    Optional<TemplateVersionEntity> findByTemplateIdAndVersionNumber(UUID templateId, int versionNumber);

    @Query("SELECT COALESCE(MAX(v.versionNumber), 0) FROM TemplateVersionEntity v WHERE v.templateId = :templateId")
    int findMaxVersionNumber(UUID templateId);
}
