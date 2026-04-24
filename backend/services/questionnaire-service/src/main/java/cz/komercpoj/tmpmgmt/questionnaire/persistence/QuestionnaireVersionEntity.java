package cz.komercpoj.tmpmgmt.questionnaire.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Immutable snapshot of a questionnaire structure at publish time. The {@code structureSnapshot}
 * column holds a JSON document shaped like the API's {@code QuestionnaireResponse} (sections
 * with embedded questions) so it round-trips through the existing mapper without an extra
 * transformation step.
 */
@Entity
@Table(name = "questionnaire_version")
@Getter
@Setter
@NoArgsConstructor
public class QuestionnaireVersionEntity {

    @Id
    private UUID id;

    @Column(name = "questionnaire_id", nullable = false)
    private UUID questionnaireId;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Column(name = "name_snapshot", nullable = false, length = 500)
    private String nameSnapshot;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "structure_snapshot", nullable = false, columnDefinition = "jsonb")
    private JsonNode structureSnapshot;

    @Column(name = "published_at", nullable = false)
    private Instant publishedAt;

    @Column(name = "published_by", nullable = false)
    private UUID publishedBy;

    public static QuestionnaireVersionEntity publish(
            UUID id,
            UUID questionnaireId,
            int versionNumber,
            String nameSnapshot,
            JsonNode structureSnapshot,
            UUID publishedBy) {
        QuestionnaireVersionEntity v = new QuestionnaireVersionEntity();
        v.id = id;
        v.questionnaireId = questionnaireId;
        v.versionNumber = versionNumber;
        v.nameSnapshot = nameSnapshot;
        v.structureSnapshot = structureSnapshot;
        v.publishedBy = publishedBy;
        v.publishedAt = Instant.now();
        return v;
    }
}
