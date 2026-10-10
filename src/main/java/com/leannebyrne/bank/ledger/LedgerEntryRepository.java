package com.leannebyrne.bank.ledger;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

    Page<LedgerEntry> findByAccountIdOrderByCreatedAtDesc(UUID accountId, Pageable pageable);

    @Query("""
            select coalesce(sum(case when e.entryType = 'CREDIT' then e.amountCents else -e.amountCents end), 0)
            from LedgerEntry e
            where e.accountId = :accountId
            """)
    long sumBalanceCentsByAccountId(@Param("accountId") UUID accountId);

    /**
     * Across every account, not just one -- the double-entry invariant (ADR
     * 0003) means this must always be exactly zero. A non-zero result here
     * would mean money was created or destroyed somewhere, which should be
     * structurally impossible given every write goes through LedgerService.
     */
    @Query("""
            select coalesce(sum(case when e.entryType = 'CREDIT' then e.amountCents else -e.amountCents end), 0)
            from LedgerEntry e
            """)
    long sumNetBalanceCents();
}
