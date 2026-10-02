package com.leannebyrne.bank.ledger.dto;

import com.leannebyrne.bank.ledger.EntryType;
import com.leannebyrne.bank.ledger.LedgerEntry;
import java.time.Instant;
import java.util.UUID;

public record LedgerEntryResponse(
        UUID id,
        UUID transferId,
        UUID accountId,
        EntryType entryType,
        long amountCents,
        Instant createdAt
) {

    public static LedgerEntryResponse from(LedgerEntry entry) {
        return new LedgerEntryResponse(
                entry.getId(),
                entry.getTransferId(),
                entry.getAccountId(),
                entry.getEntryType(),
                entry.getAmountCents(),
                entry.getCreatedAt()
        );
    }
}
