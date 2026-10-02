package com.leannebyrne.bank.account;

import com.leannebyrne.bank.account.dto.AccountResponse;
import com.leannebyrne.bank.ledger.dto.LedgerEntryResponse;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * No auth yet (Phase 4): these routes take an explicit account id rather than
 * resolving "the current customer" from a JWT principal. Phase 4 adds the JWT
 * filter and layers an ownership check onto these same routes.
 */
@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/{accountId}")
    public AccountResponse getAccount(@PathVariable UUID accountId) {
        Account account = accountService.getById(accountId);
        return AccountResponse.from(account, accountService.getBalanceCents(accountId));
    }

    @GetMapping("/{accountId}/transactions")
    public Page<LedgerEntryResponse> getTransactions(
            @PathVariable UUID accountId,
            @PageableDefault(size = 20) Pageable pageable) {
        accountService.getById(accountId);
        return accountService.getTransactionHistory(accountId, pageable).map(LedgerEntryResponse::from);
    }
}
