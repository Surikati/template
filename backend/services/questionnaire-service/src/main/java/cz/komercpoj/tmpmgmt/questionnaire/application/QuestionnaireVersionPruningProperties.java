package cz.komercpoj.tmpmgmt.questionnaire.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Knobs for the scheduled pruning job that trims old immutable questionnaire versions.
 *
 * <p>Defaults: enabled, 90-day retention, runs at 03:00 server-local time daily.
 */
@ConfigurationProperties(prefix = "tmpmgmt.questionnaire.pruning")
public record QuestionnaireVersionPruningProperties(
    Boolean enabled, Integer retentionDays, String cron) {

  public QuestionnaireVersionPruningProperties {
    if (enabled == null) enabled = true;
    if (retentionDays == null || retentionDays <= 0) retentionDays = 90;
    if (cron == null || cron.isBlank()) cron = "0 0 3 * * *";
  }
}
