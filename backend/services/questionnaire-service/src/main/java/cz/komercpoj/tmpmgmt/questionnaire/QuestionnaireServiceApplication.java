package cz.komercpoj.tmpmgmt.questionnaire;

import cz.komercpoj.tmpmgmt.questionnaire.application.QuestionnaireVersionPruningProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties(QuestionnaireVersionPruningProperties.class)
@EnableScheduling
public class QuestionnaireServiceApplication {
  public static void main(String[] args) {
    SpringApplication.run(QuestionnaireServiceApplication.class, args);
  }
}
