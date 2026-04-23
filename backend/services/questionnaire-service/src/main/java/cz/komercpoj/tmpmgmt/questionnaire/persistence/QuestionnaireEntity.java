package cz.komercpoj.tmpmgmt.questionnaire.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "questionnaire")
@Getter
@Setter
@NoArgsConstructor
public class QuestionnaireEntity {

    @Id
    private UUID id;

    @Column(name = "template_id", nullable = false)
    private UUID templateId;

    @Column(name = "template_version_number", nullable = false)
    private int templateVersionNumber;

    @Column(nullable = false, length = 500)
    private String name;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(
            mappedBy = "questionnaire",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER)
    @OrderBy("ordinal ASC")
    private List<QuestionnaireSectionEntity> sections = new ArrayList<>();

    public static QuestionnaireEntity create(
            UUID id, UUID templateId, int templateVersionNumber, String name) {
        QuestionnaireEntity q = new QuestionnaireEntity();
        Instant now = Instant.now();
        q.id = id;
        q.templateId = templateId;
        q.templateVersionNumber = templateVersionNumber;
        q.name = name;
        q.createdAt = now;
        q.updatedAt = now;
        return q;
    }

    public void touchUpdated() {
        this.updatedAt = Instant.now();
    }

    public void replaceSections(List<QuestionnaireSectionEntity> newSections) {
        this.sections.clear();
        for (QuestionnaireSectionEntity s : newSections) {
            s.setQuestionnaire(this);
            this.sections.add(s);
        }
    }
}
