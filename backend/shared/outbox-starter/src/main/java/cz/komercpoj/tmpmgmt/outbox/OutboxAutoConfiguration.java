package cz.komercpoj.tmpmgmt.outbox;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@AutoConfiguration
@ConditionalOnProperty(prefix = "tmpmgmt.outbox", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(OutboxProperties.class)
@EnableScheduling
@ComponentScan
public class OutboxAutoConfiguration {}
