package cz.komercpoj.tmpmgmt.clause.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Published, immutable clause snapshot. DB trigger rejects UPDATE regardless of ORM state.
 */
@Entity
@Table(name = "clause_version")
@Immutable
@Getter
@NoArgsConstructor
public class ClauseVersionEntity {

    @Id
    private UUID id;

    @Column(name = "clause_id", nullable = false)
    private UUID clauseId;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private JsonNode content;

    @Column(name = "change_note", columnDefinition = "TEXT")
    private String changeNote;

    @Column(name = "published_at", nullable = false)
    private Instant publishedAt;

    @Column(name = "published_by", nullable = false)
    private UUID publishedBy;

    public static ClauseVersionEntity publish(
            UUID id,
            UUID clauseId,
            int versionNumber,
            JsonNode content,
            String changeNote,
            UUID publishedBy) {
        ClauseVersionEntity v = new ClauseVersionEntity();
        v.id = id;
        v.clauseId = clauseId;
        v.versionNumber = versionNumber;
        v.content = content;
        v.changeNote = changeNote;
        v.publishedAt = Instant.now();
        v.publishedBy = publishedBy;
        return v;
    }
}
