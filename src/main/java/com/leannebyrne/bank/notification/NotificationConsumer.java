package com.leannebyrne.bank.notification;

import com.leannebyrne.bank.outbox.KafkaTopics;
import com.leannebyrne.bank.transfer.TransferCompletedEvent;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Simulated notification service -- there is no real email/SMS integration
 * here, just a log line standing in for "a message was sent," which is
 * enough to demonstrate the pattern without needing a third-party account.
 * This is its own Kafka consumer group ("notification"), separate from
 * AuditLogConsumer's ("audit-log") group -- each group gets its own
 * independent copy of every message, which is how one event fans out to
 * multiple unrelated consumers without either one knowing the other exists.
 *
 * Disabled under the "cloud" profile, same as OutboxRelay -- see ADR 0011.
 */
@Component
@Profile("!cloud")
public class NotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);

    private final NotificationLogRepository notificationLogRepository;
    private final ObjectMapper objectMapper;

    public NotificationConsumer(NotificationLogRepository notificationLogRepository, ObjectMapper objectMapper) {
        this.notificationLogRepository = notificationLogRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = KafkaTopics.TRANSFER_COMPLETED, groupId = "notification")
    public void onTransferCompleted(@Payload String payload, @Header("eventId") String eventId) {
        TransferCompletedEvent event = objectMapper.readValue(payload, TransferCompletedEvent.class);
        UUID id = UUID.fromString(eventId);

        notify(id, event.sourceCustomerId(),
                "Your account was debited $%s".formatted(centsToDisplay(event.amountCents())));
        notify(id, event.destinationCustomerId(),
                "Your account was credited $%s".formatted(centsToDisplay(event.amountCents())));
    }

    private void notify(UUID eventId, UUID customerId, String message) {
        try {
            notificationLogRepository.save(new NotificationLogEntry(eventId, customerId, message));
            log.info("Notified customer {}: {}", customerId, message);
        } catch (DataIntegrityViolationException alreadyNotified) {
            log.info("Customer {} already notified for event {}, skipping duplicate delivery", customerId, eventId);
        }
    }

    private String centsToDisplay(long amountCents) {
        return "%d.%02d".formatted(amountCents / 100, amountCents % 100);
    }
}
