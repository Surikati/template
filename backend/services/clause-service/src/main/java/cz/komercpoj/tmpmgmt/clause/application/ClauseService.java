package cz.komercpoj.tmpmgmt.clause.application;

import cz.komercpoj.tmpmgmt.clause.application.ClauseCommands.Archive;
import cz.komercpoj.tmpmgmt.clause.application.ClauseCommands.CreateClause;
import cz.komercpoj.tmpmgmt.clause.application.ClauseCommands.PublishVersion;
import cz.komercpoj.tmpmgmt.clause.application.events.ClauseEvents;
import cz.komercpoj.tmpmgmt.clause.domain.ClauseStatus;
import cz.komercpoj.tmpmgmt.clause.persistence.ClauseEntity;
import cz.komercpoj.tmpmgmt.clause.persistence.ClauseRepository;
import cz.komercpoj.tmpmgmt.clause.persistence.ClauseVersionEntity;
import cz.komercpoj.tmpmgmt.clause.persistence.ClauseVersionRepository;
import cz.komercpoj.tmpmgmt.common.ConflictException;
import cz.komercpoj.tmpmgmt.common.NotFoundException;
import cz.komercpoj.tmpmgmt.outbox.OutboxWriter;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClauseService {

    private final ClauseRepository clauses;
    private final ClauseVersionRepository versions;
    private final ClauseContentValidator validator;
    private final OutboxWriter outbox;

    public ClauseService(
            ClauseRepository clauses,
            ClauseVersionRepository versions,
            ClauseContentValidator validator,
            OutboxWriter outbox) {
        this.clauses = clauses;
        this.versions = versions;
        this.validator = validator;
        this.outbox = outbox;
    }

    @Transactional
    public ClauseEntity create(CreateClause cmd) {
        if (clauses.existsBySlug(cmd.slug())) {
            throw new ConflictException(
                    "clause.slug_taken", "Slug already in use: " + cmd.slug());
        }
        UUID id = UUID.randomUUID();
        ClauseEntity c = ClauseEntity.newActive(
                id, cmd.slug(), cmd.name(), cmd.description(), cmd.category(), cmd.ownerUserId());
        clauses.save(c);

        outbox.stage(
                ClauseEvents.AGGREGATE_TYPE,
                id.toString(),
                ClauseEvents.TYPE_CREATED,
                new ClauseEvents.ClauseCreated(
                        id,
                        cmd.slug(),
                        cmd.name(),
                        cmd.description(),
                        cmd.category(),
                        List.of(c.getTags()),
                        cmd.ownerUserId(),
                        Instant.now()));
        return c;
    }

    @Transactional(readOnly = true)
    public ClauseEntity getById(UUID id) {
        return clauses.findById(id).orElseThrow(() -> notFound(id));
    }

    @Transactional(readOnly = true)
    public List<ClauseEntity> list() {
        return clauses.findAll(Sort.by(Sort.Direction.DESC, "updatedAt"));
    }

    @Transactional(readOnly = true)
    public List<ClauseVersionEntity> listVersions(UUID clauseId) {
        getById(clauseId); // 404 vs. empty list
        return versions.findByClauseIdOrderByVersionNumberDesc(clauseId);
    }

    @Transactional(readOnly = true)
    public ClauseVersionEntity getVersion(UUID clauseId, int versionNumber) {
        return versions.findByClauseIdAndVersionNumber(clauseId, versionNumber)
                .orElseThrow(() -> new NotFoundException(
                        "clause.version_not_found",
                        "No version " + versionNumber + " for clause " + clauseId));
    }

    @Transactional
    public ClauseVersionEntity publishVersion(PublishVersion cmd) {
        ClauseEntity clause = getById(cmd.clauseId());
        if (clause.getStatus() == ClauseStatus.ARCHIVED) {
            throw new ConflictException(
                    "clause.archived", "Cannot publish archived clause " + clause.getId());
        }
        validator.validate(cmd.content());

        int nextVersion = versions.findMaxVersionNumber(clause.getId()) + 1;
        UUID versionId = UUID.randomUUID();
        ClauseVersionEntity v = ClauseVersionEntity.publish(
                versionId, clause.getId(), nextVersion, cmd.content(), cmd.changeNote(), cmd.publishedBy());
        versions.save(v);

        clause.touchUpdated();

        outbox.stage(
                ClauseEvents.AGGREGATE_TYPE,
                clause.getId().toString(),
                ClauseEvents.TYPE_VERSION_PUBLISHED,
                new ClauseEvents.ClauseVersionPublished(
                        clause.getId(),
                        versionId,
                        nextVersion,
                        cmd.changeNote(),
                        cmd.publishedBy(),
                        Instant.now()));
        return v;
    }

    @Transactional
    public void archive(Archive cmd) {
        ClauseEntity clause = getById(cmd.clauseId());
        if (clause.getStatus() == ClauseStatus.ARCHIVED) {
            return;
        }
        clause.setStatus(ClauseStatus.ARCHIVED);
        clause.touchUpdated();

        outbox.stage(
                ClauseEvents.AGGREGATE_TYPE,
                clause.getId().toString(),
                ClauseEvents.TYPE_ARCHIVED,
                new ClauseEvents.ClauseArchived(
                        clause.getId(), cmd.actorUserId(), Instant.now()));
    }

    private NotFoundException notFound(UUID id) {
        return new NotFoundException("clause.not_found", "Clause not found: " + id);
    }
}
