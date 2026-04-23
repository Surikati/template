package cz.komercpoj.tmpmgmt.clause.persistence;

import cz.komercpoj.tmpmgmt.clause.domain.ClauseStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClauseRepository extends JpaRepository<ClauseEntity, UUID> {

    Optional<ClauseEntity> findBySlug(String slug);

    boolean existsBySlug(String slug);

    Page<ClauseEntity> findByStatus(ClauseStatus status, Pageable pageable);

    Page<ClauseEntity> findByStatusAndCategory(ClauseStatus status, String category, Pageable pageable);
}
