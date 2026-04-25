package cz.komercpoj.tmpmgmt.expression;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class ExpressionAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public ExpressionEvaluator expressionEvaluator() {
    return new AntlrExpressionEvaluator();
  }
}
