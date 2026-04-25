package cz.komercpoj.tmpmgmt.audit.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "tmpmgmt.audit")
public class RabbitTopology {

  private String exchange = "tmpmgmt.events";
  private String queue = "audit.all-events";
  private String bindingPattern = "#";

  public void setExchange(String exchange) {
    this.exchange = exchange;
  }

  public void setQueue(String queue) {
    this.queue = queue;
  }

  public void setBindingPattern(String bindingPattern) {
    this.bindingPattern = bindingPattern;
  }

  public String getQueue() {
    return queue;
  }

  @Bean
  TopicExchange auditExchange() {
    return new TopicExchange(exchange, /*durable*/ true, /*autoDelete*/ false);
  }

  @Bean
  Queue auditQueue() {
    return new Queue(queue, true);
  }

  @Bean
  Binding auditBinding(Queue auditQueue, TopicExchange auditExchange) {
    return BindingBuilder.bind(auditQueue).to(auditExchange).with(bindingPattern);
  }
}
