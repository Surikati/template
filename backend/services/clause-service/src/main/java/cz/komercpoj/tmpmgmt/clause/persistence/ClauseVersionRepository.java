package cz.komercpoj.tmpmgmt.clause.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ClauseVersionRepository extends JpaRepository<ClauseVersionEntity, UUID> {

    List<ClauseVersionEntity> findByClauseIdOrderByVersionNumberDesc(UUID clauseId);

    Optional<ClauseVersionEntity> findByClauseIdAndVersionNumber(UUID clauseId, int versionNumber);

    @Query("SELECT COALESCE(MAX(v.versionNumber), 0) FROM ClauseVersionEntity v WHERE v.clauseId = :clauseId")
    int findMaxVersionNumber(UUID clauseId);
}
