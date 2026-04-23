package cz.komercpoj.tmpmgmt.template.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TemplateDraftRepository extends JpaRepository<TemplateDraftEntity, UUID> {}
