package cz.komercpoj.tmpmgmt.outbox;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tmpmgmt.outbox")
public record OutboxProperties(
        String exchange, Duration pollInterval, int batchSize, boolean publisherEnabled) {

    public OutboxProperties {
        if (exchange == null) exchange = "tmpmgmt.events";
        if (pollInterval == null) pollInterval = Duration.ofSeconds(2);
        if (batchSize <= 0) batchSize = 100;
    }
}
