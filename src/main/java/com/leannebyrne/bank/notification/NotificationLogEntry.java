package com.leannebyrne.bank.notification;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * One event can produce one row per affected customer (a transfer has both
 * a source and a destination owner), so the dedup key is (eventId,
 * customerId) together, not eventId alone -- see the unique constraint in
 * V3__create_outbox_and_consumer_logs.sql.
 */
@Entity
@Table(name = "notification_log")
@EntityListeners(AuditingEntityListener.class)
public class NotificationLogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID eventId;

    private UUID customerId;

    private String message;

    @CreatedDate
    private Instant sentAt;

    protected NotificationLogEntry() {
    }

    public NotificationLogEntry(UUID eventId, UUID customerId, String message) {
        this.eventId = eventId;
        this.customerId = customerId;
        this.message = message;
    }

    public UUID getId() {
        return id;
    }

    public UUID getEventId() {
        return eventId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public String getMessage() {
        return message;
    }

    public Instant getSentAt() {
        return sentAt;
    }
}
