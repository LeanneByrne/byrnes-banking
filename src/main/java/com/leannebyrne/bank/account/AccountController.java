package com.leannebyrne.bank.account;

import com.leannebyrne.bank.account.dto.AccountResponse;
import com.leannebyrne.bank.ledger.dto.LedgerEntryResponse;
import com.leannebyrne.bank.security.CurrentUser;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Account ids stay explicit path variables rather than a {@code /me} route;
 * ownership (is the authenticated principal allowed to see THIS account) is
 * enforced in AccountService, not here.
 */
@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/{accountId}")
    public AccountResponse getAccount(@PathVariable UUID accountId, @AuthenticationPrincipal CurrentUser currentUser) {
        Account account = accountService.getById(accountId, currentUser.customerId(), currentUser.role());
        return AccountResponse.from(account, accountService.getBalanceCents(accountId));
    }

    @GetMapping("/{accountId}/transactions")
    public Page<LedgerEntryResponse> getTransactions(
            @PathVariable UUID accountId,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal CurrentUser currentUser) {
        return accountService
                .getTransactionHistory(accountId, pageable, currentUser.customerId(), currentUser.role())
                .map(LedgerEntryResponse::from);
    }
}
