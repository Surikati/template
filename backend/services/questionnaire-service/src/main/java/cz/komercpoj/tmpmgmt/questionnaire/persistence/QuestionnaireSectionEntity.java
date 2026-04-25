package cz.komercpoj.tmpmgmt.questionnaire.persistence;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "questionnaire_section")
@Getter
@Setter
@NoArgsConstructor
public class QuestionnaireSectionEntity implements Persistable<UUID> {

  @Id private UUID id;

  /**
   * Forces cascade-saved sections through {@code persist()} instead of {@code merge()} so each
   * insert is one DB roundtrip rather than a SELECT-then-INSERT.
   */
  @Transient private boolean isNew = true;

  @Override
  public boolean isNew() {
    return isNew;
  }

  @PostPersist
  @PostLoad
  void markNotNew() {
    this.isNew = false;
  }

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "questionnaire_id")
  private QuestionnaireEntity questionnaire;

  @Column(nullable = false)
  private int ordinal;

  @Column(nullable = false, length = 500)
  private String title;

  @Column(name = "visibility_rule", columnDefinition = "TEXT")
  private String visibilityRule;

  @OneToMany(
      mappedBy = "section",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.EAGER)
  @OrderBy("ordinal ASC")
  private List<QuestionnaireQuestionEntity> questions = new ArrayList<>();

  public static QuestionnaireSectionEntity create(
      UUID id, int ordinal, String title, String visibilityRule) {
    QuestionnaireSectionEntity s = new QuestionnaireSectionEntity();
    s.id = id;
    s.ordinal = ordinal;
    s.title = title;
    s.visibilityRule = visibilityRule;
    return s;
  }

  public void addQuestion(QuestionnaireQuestionEntity q) {
    q.setSection(this);
    this.questions.add(q);
  }
}
