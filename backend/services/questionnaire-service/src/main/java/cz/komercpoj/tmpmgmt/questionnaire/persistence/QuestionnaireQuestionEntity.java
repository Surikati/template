package cz.komercpoj.tmpmgmt.questionnaire.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import cz.komercpoj.tmpmgmt.questionnaire.domain.QuestionType;
import jakarta.persistence.*;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "questionnaire_question")
@Getter
@Setter
@NoArgsConstructor
public class QuestionnaireQuestionEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "section_id")
    private QuestionnaireSectionEntity section;

    @Column(nullable = false)
    private int ordinal;

    @Column(name = "variable_path", nullable = false, length = 500)
    private String variablePath;

    @Column(nullable = false, length = 1000)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, length = 30)
    private QuestionType questionType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode validation;

    @Column(name = "visibility_rule", columnDefinition = "TEXT")
    private String visibilityRule;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode options;

    public static QuestionnaireQuestionEntity create(
            UUID id,
            int ordinal,
            String variablePath,
            String label,
            QuestionType type,
            JsonNode validation,
            String visibilityRule,
            JsonNode options) {
        QuestionnaireQuestionEntity q = new QuestionnaireQuestionEntity();
        q.id = id;
        q.ordinal = ordinal;
        q.variablePath = variablePath;
        q.label = label;
        q.questionType = type;
        q.validation = validation;
        q.visibilityRule = visibilityRule;
        q.options = options;
        return q;
    }
}
