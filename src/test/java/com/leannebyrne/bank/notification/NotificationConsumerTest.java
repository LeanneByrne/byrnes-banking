package com.leannebyrne.bank.notification;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.leannebyrne.bank.transfer.TransferCompletedEvent;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class NotificationConsumerTest {

    @Mock
    private NotificationLogRepository notificationLogRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void notifiesBothSourceAndDestinationCustomers() {
        NotificationConsumer consumer = new NotificationConsumer(notificationLogRepository, objectMapper);
        UUID eventId = UUID.randomUUID();
        UUID sourceCustomerId = UUID.randomUUID();
        UUID destinationCustomerId = UUID.randomUUID();
        String payload = objectMapper.writeValueAsString(new TransferCompletedEvent(
                UUID.randomUUID(), UUID.randomUUID(), sourceCustomerId,
                UUID.randomUUID(), destinationCustomerId,
                2_500L, "USD", Instant.now()));

        consumer.onTransferCompleted(payload, eventId.toString());

        verify(notificationLogRepository).save(argThat(n -> n.getCustomerId().equals(sourceCustomerId)));
        verify(notificationLogRepository).save(argThat(n -> n.getCustomerId().equals(destinationCustomerId)));
    }

    /**
     * Same at-least-once reasoning as AuditLogConsumerTest: a duplicate
     * delivery notifying one of the two customers again must not crash the
     * listener or prevent the other customer's (also-duplicate) attempt
     * from being handled the same way.
     */
    @Test
    void duplicateDeliveryPerCustomerIsSwallowedNotRethrown() {
        NotificationConsumer consumer = new NotificationConsumer(notificationLogRepository, objectMapper);
        UUID eventId = UUID.randomUUID();
        String payload = objectMapper.writeValueAsString(new TransferCompletedEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(),
                1_000L, "USD", Instant.now()));
        when(notificationLogRepository.save(any())).thenThrow(new DataIntegrityViolationException("duplicate"));

        consumer.onTransferCompleted(payload, eventId.toString());

        verify(notificationLogRepository, times(2)).save(any());
    }
}
