package com.leannebyrne.bank.audit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class AuditLogConsumerTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Test
    void recordsEventOnFirstDelivery() {
        AuditLogConsumer consumer = new AuditLogConsumer(auditLogRepository);
        UUID eventId = UUID.randomUUID();

        consumer.onTransferCompleted("{\"amountCents\":500}", eventId.toString(), "TransferCompleted");

        verify(auditLogRepository).save(any());
    }

    /**
     * The outbox relay guarantees at-least-once delivery (see OutboxRelay),
     * so the same event arriving twice is an expected occurrence, not a
     * bug -- this proves the duplicate is swallowed rather than propagated
     * as an error that would, for example, crash the Kafka listener thread.
     */
    @Test
    void duplicateDeliveryIsSwallowedNotRethrown() {
        AuditLogConsumer consumer = new AuditLogConsumer(auditLogRepository);
        UUID eventId = UUID.randomUUID();
        when(auditLogRepository.save(any())).thenThrow(new DataIntegrityViolationException("duplicate event_id"));

        consumer.onTransferCompleted("{\"amountCents\":500}", eventId.toString(), "TransferCompleted");
    }
}
