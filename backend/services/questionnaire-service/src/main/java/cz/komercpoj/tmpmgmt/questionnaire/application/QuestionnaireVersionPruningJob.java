package cz.komercpoj.tmpmgmt.questionnaire.application;

import cz.komercpoj.tmpmgmt.questionnaire.persistence.QuestionnaireVersionRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Daily housekeeping job that deletes immutable {@code questionnaire_version} rows older than the
 * configured retention period. The latest version per questionnaire is always preserved, even when
 * older than the retention threshold, so a long-quiet questionnaire keeps a usable snapshot.
 *
 * <p>The job is a no-op when {@code tmpmgmt.questionnaire.pruning.enabled=false} (the bean is not
 * registered at all). Cron defaults to 03:00 server-local time; tune via {@code
 * tmpmgmt.questionnaire.pruning.cron} on noisy clusters.
 */
@Component
@ConditionalOnProperty(
    prefix = "tmpmgmt.questionnaire.pruning",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class QuestionnaireVersionPruningJob {

  private static final Logger log = LoggerFactory.getLogger(QuestionnaireVersionPruningJob.class);

  private final QuestionnaireVersionRepository versions;
  private final QuestionnaireVersionPruningProperties props;

  public QuestionnaireVersionPruningJob(
      QuestionnaireVersionRepository versions, QuestionnaireVersionPruningProperties props) {
    this.versions = versions;
    this.props = props;
  }

  @Scheduled(cron = "${tmpmgmt.questionnaire.pruning.cron:0 0 3 * * *}")
  @Transactional
  public PruneResult run() {
    Instant cutoff = Instant.now().minus(props.retentionDays(), ChronoUnit.DAYS);
    int deleted = versions.deleteOlderThanKeepingLatest(cutoff);
    if (deleted > 0) {
      log.info(
          "Pruned {} questionnaire_version row(s) older than {} (retention {} days)",
          deleted,
          cutoff,
          props.retentionDays());
    } else {
      log.debug(
          "No questionnaire_version rows to prune (retention {} days, cutoff {})",
          props.retentionDays(),
          cutoff);
    }
    return new PruneResult(cutoff, deleted);
  }

  public record PruneResult(Instant cutoff, int deleted) {}
}
