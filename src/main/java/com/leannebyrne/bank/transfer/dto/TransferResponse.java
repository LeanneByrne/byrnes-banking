package com.leannebyrne.bank.transfer.dto;

import com.leannebyrne.bank.transfer.Transfer;
import com.leannebyrne.bank.transfer.TransferStatus;
import java.time.Instant;
import java.util.UUID;

public record TransferResponse(
        UUID id,
        UUID sourceAccountId,
        UUID destinationAccountId,
        long amountCents,
        String currency,
        TransferStatus status,
        String idempotencyKey,
        String failureReason,
        Instant createdAt,
        Instant completedAt
) {

    public static TransferResponse from(Transfer transfer) {
        return new TransferResponse(
                transfer.getId(),
                transfer.getSourceAccountId(),
                transfer.getDestinationAccountId(),
                transfer.getAmountCents(),
                transfer.getCurrency(),
                transfer.getStatus(),
                transfer.getIdempotencyKey(),
                transfer.getFailureReason(),
                transfer.getCreatedAt(),
                transfer.getCompletedAt()
        );
    }
}
