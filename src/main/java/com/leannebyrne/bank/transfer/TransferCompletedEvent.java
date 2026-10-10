package com.leannebyrne.bank.transfer;

import java.time.Instant;
import java.util.UUID;

/**
 * The outbox/Kafka payload shape for a completed transfer. Carries both
 * accounts' owning customer ids directly (rather than making consumers look
 * them up) since TransferService already has both Account objects loaded in
 * memory when it builds this -- the lookup is free here and costs a query
 * anywhere else.
 */
public record TransferCompletedEvent(
        UUID transferId,
        UUID sourceAccountId,
        UUID sourceCustomerId,
        UUID destinationAccountId,
        UUID destinationCustomerId,
        long amountCents,
        String currency,
        Instant completedAt
) {
    public static final String EVENT_TYPE = "TransferCompleted";
}
