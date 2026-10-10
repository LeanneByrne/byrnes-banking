package com.leannebyrne.bank.audit;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Deliberately generic -- an audit trail's job is to record that an event
 * happened and what it said, not to understand its business meaning, so
 * this consumer (unlike NotificationConsumer) never deserializes the
 * payload into a specific event type.
 */
@Entity
@Table(name = "audit_log")
@EntityListeners(AuditingEntityListener.class)
public class AuditLogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID eventId;

    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    private String payload;

    @CreatedDate
    private Instant recordedAt;

    protected AuditLogEntry() {
    }

    public AuditLogEntry(UUID eventId, String eventType, String payload) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.payload = payload;
    }

    public UUID getId() {
        return id;
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }
}
