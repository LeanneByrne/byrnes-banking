package com.leannebyrne.bank.ledger;

import com.leannebyrne.bank.ledger.dto.LedgerBalanceCheckResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ADMIN-only (see SecurityConfig) -- this is a whole-ledger integrity check,
 * not a customer-facing balance, so it has no ownership concept at all.
 */
@RestController
@RequestMapping("/api/ledger")
public class LedgerController {

    private final LedgerService ledgerService;

    public LedgerController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @GetMapping("/balance-check")
    public LedgerBalanceCheckResponse balanceCheck() {
        return LedgerBalanceCheckResponse.of(ledgerService.getNetBalanceCents());
    }
}
