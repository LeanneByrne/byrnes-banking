package com.leannebyrne.bank.ledger.dto;

public record LedgerBalanceCheckResponse(long netCents, boolean balanced) {

    public static LedgerBalanceCheckResponse of(long netCents) {
        return new LedgerBalanceCheckResponse(netCents, netCents == 0);
    }
}
