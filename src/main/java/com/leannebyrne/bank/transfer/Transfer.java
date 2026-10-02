package com.leannebyrne.bank.transfer;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "transfers")
@EntityListeners(AuditingEntityListener.class)
public class Transfer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID sourceAccountId;

    private UUID destinationAccountId;

    private long amountCents;

    private String currency;

    @Enumerated(EnumType.STRING)
    private TransferStatus status;

    private String idempotencyKey;

    private String failureReason;

    @CreatedDate
    private Instant createdAt;

    private Instant completedAt;

    protected Transfer() {
    }

    public Transfer(UUID sourceAccountId, UUID destinationAccountId, long amountCents, String currency,
            String idempotencyKey) {
        this.sourceAccountId = sourceAccountId;
        this.destinationAccountId = destinationAccountId;
        this.amountCents = amountCents;
        this.currency = currency;
        this.idempotencyKey = idempotencyKey;
        this.status = TransferStatus.PENDING;
    }

    public void markCompleted() {
        this.status = TransferStatus.COMPLETED;
        this.completedAt = Instant.now();
    }

    public void markFailed(String reason) {
        this.status = TransferStatus.FAILED;
        this.failureReason = reason;
        this.completedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getSourceAccountId() {
        return sourceAccountId;
    }

    public UUID getDestinationAccountId() {
        return destinationAccountId;
    }

    public long getAmountCents() {
        return amountCents;
    }

    public String getCurrency() {
        return currency;
    }

    public TransferStatus getStatus() {
        return status;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}
