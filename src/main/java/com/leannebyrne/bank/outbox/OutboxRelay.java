package com.leannebyrne.bank.outbox;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Disabled under the "cloud" profile -- see ADR 0011. TransferService still
 * writes outbox_events rows unconditionally in every environment (a single
 * code path for the thing that actually matters, money movement); in the
 * cloud deployment those rows simply accumulate unpublished, which is inert
 * and documented, not a silent gap.
 *
 * Polls for outbox rows nobody has published yet and sends each one to
 * Kafka, stamping publishedAt only after the send is acknowledged. eventId
 * and eventType travel as record headers, not inside the JSON value, so the
 * value is exactly the business payload a consumer cares about -- this is
 * the standard shape for the outbox pattern (metadata in headers, payload
 * in the value), not a project-specific choice.
 *
 * A send that throws leaves the row unpublished, so the next poll retries
 * it -- this is what makes delivery "at-least-once" rather than "at-most-
 * once": a crash between a successful send and the publishedAt write would
 * also retry, meaning a consumer can see the same event twice. Consumers
 * are required to be idempotent (see AuditLogConsumer, NotificationConsumer)
 * specifically because of this guarantee, not despite it.
 */
@Component
@Profile("!cloud")
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxRelay(OutboxEventRepository outboxEventRepository, KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelay = 2000)
    @Transactional
    public void publishPending() {
        List<OutboxEvent> pending = outboxEventRepository.findTop50ByPublishedAtIsNullOrderByCreatedAtAsc();
        for (OutboxEvent event : pending) {
            publish(event);
        }
    }

    private void publish(OutboxEvent event) {
        String topic = topicFor(event.getEventType());
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, event.getId().toString(), event.getPayload());
        record.headers().add("eventId", event.getId().toString().getBytes(StandardCharsets.UTF_8));
        record.headers().add("eventType", event.getEventType().getBytes(StandardCharsets.UTF_8));

        try {
            kafkaTemplate.send(record).get(5, TimeUnit.SECONDS);
            event.markPublished();
        } catch (Exception e) {
            // Left unpublished on purpose -- the next poll retries it. Logged
            // rather than rethrown so one stuck event doesn't block every
            // other row in the batch behind it.
            log.warn("Failed to publish outbox event {}, will retry next poll", event.getId(), e);
        }
    }

    private String topicFor(String eventType) {
        return switch (eventType) {
            case "TransferCompleted" -> KafkaTopics.TRANSFER_COMPLETED;
            default -> throw new IllegalStateException("No topic mapped for event type: " + eventType);
        };
    }
}
