package com.leannebyrne.bank.ledger;

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

/**
 * Immutable, append-only. No setters, no update/delete repository methods —
 * corrections are new reversing entries, never edits to an existing row.
 */
@Entity
@Table(name = "ledger_entries")
@EntityListeners(AuditingEntityListener.class)
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID transferId;

    private UUID accountId;

    @Enumerated(EnumType.STRING)
    private EntryType entryType;

    private long amountCents;

    @CreatedDate
    private Instant createdAt;

    protected LedgerEntry() {
    }

    public LedgerEntry(UUID transferId, UUID accountId, EntryType entryType, long amountCents) {
        this.transferId = transferId;
        this.accountId = accountId;
        this.entryType = entryType;
        this.amountCents = amountCents;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTransferId() {
        return transferId;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public EntryType getEntryType() {
        return entryType;
    }

    public long getAmountCents() {
        return amountCents;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
