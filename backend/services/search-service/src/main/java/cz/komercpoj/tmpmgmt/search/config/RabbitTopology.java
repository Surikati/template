package cz.komercpoj.tmpmgmt.search.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitTopology {

  public static final String EXCHANGE = "tmpmgmt.events";
  public static final String QUEUE_TEMPLATE = "search.template-events";
  public static final String QUEUE_CLAUSE = "search.clause-events";

  @Bean
  TopicExchange tmpmgmtEventsExchange() {
    return new TopicExchange(EXCHANGE, true, false);
  }

  @Bean
  Queue templateSearchQueue() {
    return new Queue(QUEUE_TEMPLATE, true);
  }

  @Bean
  Queue clauseSearchQueue() {
    return new Queue(QUEUE_CLAUSE, true);
  }

  @Bean
  Binding templateBinding(Queue templateSearchQueue, TopicExchange tmpmgmtEventsExchange) {
    return BindingBuilder.bind(templateSearchQueue).to(tmpmgmtEventsExchange).with("template.#");
  }

  @Bean
  Binding clauseBinding(Queue clauseSearchQueue, TopicExchange tmpmgmtEventsExchange) {
    return BindingBuilder.bind(clauseSearchQueue).to(tmpmgmtEventsExchange).with("clause.#");
  }
}
