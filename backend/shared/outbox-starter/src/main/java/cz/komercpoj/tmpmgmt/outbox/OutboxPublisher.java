package cz.komercpoj.tmpmgmt.outbox;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Polls the {@code outbox_event} table and publishes unsent events to RabbitMQ. Uses SELECT … FOR
 * UPDATE SKIP LOCKED so multiple instances can run concurrently.
 */
@Component
public class OutboxPublisher {

  private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

  private static final String SELECT_SQL =
      "SELECT event_id, aggregate_type, aggregate_id, event_type, payload, occurred_at "
          + "FROM outbox_event "
          + "WHERE published_at IS NULL "
          + "ORDER BY occurred_at ASC "
          + "LIMIT ? "
          + "FOR UPDATE SKIP LOCKED";

  private static final String MARK_PUBLISHED_SQL =
      "UPDATE outbox_event SET published_at = ? WHERE event_id = ?";

  private final JdbcTemplate jdbc;
  private final RabbitTemplate rabbit;
  private final OutboxProperties props;

  public OutboxPublisher(JdbcTemplate jdbc, RabbitTemplate rabbit, OutboxProperties props) {
    this.jdbc = jdbc;
    this.rabbit = rabbit;
    this.props = props;
  }

  @Scheduled(fixedDelayString = "${tmpmgmt.outbox.poll-interval:PT2S}")
  @Transactional
  public void drain() {
    if (!props.publisherEnabled()) {
      return;
    }
    List<OutboxEvent> batch =
        jdbc.query(
            SELECT_SQL,
            (rs, n) ->
                new OutboxEvent(
                    UUID.fromString(rs.getString("event_id")),
                    rs.getString("aggregate_type"),
                    rs.getString("aggregate_id"),
                    rs.getString("event_type"),
                    rs.getString("payload"),
                    rs.getTimestamp("occurred_at").toInstant(),
                    null),
            props.batchSize());

    for (OutboxEvent evt : batch) {
      String routingKey = evt.aggregateType() + "." + evt.eventType();
      rabbit.convertAndSend(
          props.exchange(),
          routingKey,
          evt.payload(),
          m -> {
            m.getMessageProperties().setMessageId(evt.eventId().toString());
            m.getMessageProperties().setContentType("application/json");
            m.getMessageProperties().setHeader("event-type", evt.eventType());
            m.getMessageProperties().setHeader("aggregate-type", evt.aggregateType());
            m.getMessageProperties().setHeader("aggregate-id", evt.aggregateId());
            return m;
          });
      jdbc.update(MARK_PUBLISHED_SQL, Timestamp.from(Instant.now()), evt.eventId());
    }

    if (!batch.isEmpty()) {
      log.debug("Published {} outbox events to {}", batch.size(), props.exchange());
    }
  }
}
