package com.leannebyrne.bank.account;

import com.leannebyrne.bank.common.OwnershipException;
import com.leannebyrne.bank.customer.Role;
import com.leannebyrne.bank.ledger.LedgerEntry;
import com.leannebyrne.bank.ledger.LedgerEntryRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public AccountService(AccountRepository accountRepository, LedgerEntryRepository ledgerEntryRepository) {
        this.accountRepository = accountRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @Transactional(readOnly = true)
    public Account getById(UUID accountId, UUID actingCustomerId, Role actingRole) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
        requireOwnership(account.getCustomerId(), actingCustomerId, actingRole);
        return account;
    }

    @Transactional(readOnly = true)
    public List<Account> getByCustomerId(UUID customerId, UUID actingCustomerId, Role actingRole) {
        requireOwnership(customerId, actingCustomerId, actingRole);
        return accountRepository.findByCustomerId(customerId);
    }

    /**
     * Unchanged by the Phase 4 ownership retrofit on purpose: this is an
     * internal lookup TransferService uses for any account regardless of who
     * is acting, not a user-facing read. Ownership is enforced at the
     * endpoints that actually hand a balance back to an HTTP caller.
     */
    @Transactional(readOnly = true)
    public long getBalanceCents(UUID accountId) {
        return ledgerEntryRepository.sumBalanceCentsByAccountId(accountId);
    }

    @Transactional(readOnly = true)
    public Page<LedgerEntry> getTransactionHistory(UUID accountId, Pageable pageable, UUID actingCustomerId,
            Role actingRole) {
        getById(accountId, actingCustomerId, actingRole);
        return ledgerEntryRepository.findByAccountIdOrderByCreatedAtDesc(accountId, pageable);
    }

    private void requireOwnership(UUID resourceCustomerId, UUID actingCustomerId, Role actingRole) {
        if (actingRole != Role.ADMIN && !resourceCustomerId.equals(actingCustomerId)) {
            throw new OwnershipException("You do not have access to this account");
        }
    }
}
