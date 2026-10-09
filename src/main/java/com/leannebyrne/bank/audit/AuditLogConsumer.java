package com.leannebyrne.bank.audit;

import com.leannebyrne.bank.outbox.KafkaTopics;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Idempotent via the same backstop TransferService's idempotency key uses:
 * a UNIQUE constraint on event_id catches a duplicate delivery as an insert
 * conflict, not a duplicate row. This matters here specifically because the
 * outbox relay only guarantees at-least-once delivery -- seeing the same
 * event twice is an expected, normal occurrence, not a bug to chase.
 */
@Component
public class AuditLogConsumer {

    private static final Logger log = LoggerFactory.getLogger(AuditLogConsumer.class);

    private final AuditLogRepository auditLogRepository;

    public AuditLogConsumer(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @KafkaListener(topics = KafkaTopics.TRANSFER_COMPLETED, groupId = "audit-log")
    public void onTransferCompleted(
            @Payload String payload,
            @Header("eventId") String eventId,
            @Header("eventType") String eventType) {
        try {
            auditLogRepository.save(new AuditLogEntry(UUID.fromString(eventId), eventType, payload));
        } catch (DataIntegrityViolationException alreadyRecorded) {
            log.info("Event {} already recorded, skipping duplicate delivery", eventId);
        }
    }
}
