package com.leannebyrne.bank.account;

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
    public Account getById(UUID accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
    }

    @Transactional(readOnly = true)
    public List<Account> getByCustomerId(UUID customerId) {
        return accountRepository.findByCustomerId(customerId);
    }

    @Transactional(readOnly = true)
    public long getBalanceCents(UUID accountId) {
        return ledgerEntryRepository.sumBalanceCentsByAccountId(accountId);
    }

    @Transactional(readOnly = true)
    public Page<LedgerEntry> getTransactionHistory(UUID accountId, Pageable pageable) {
        return ledgerEntryRepository.findByAccountIdOrderByCreatedAtDesc(accountId, pageable);
    }
}
