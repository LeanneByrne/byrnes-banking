package com.leannebyrne.bank.ledger;

import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class LedgerService {

    private final LedgerEntryRepository ledgerEntryRepository;

    public LedgerService(LedgerEntryRepository ledgerEntryRepository) {
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    public void recordTransfer(UUID transferId, UUID sourceAccountId, UUID destinationAccountId, long amountCents) {
        ledgerEntryRepository.save(new LedgerEntry(transferId, sourceAccountId, EntryType.DEBIT, amountCents));
        ledgerEntryRepository.save(new LedgerEntry(transferId, destinationAccountId, EntryType.CREDIT, amountCents));
    }

    public long getNetBalanceCents() {
        return ledgerEntryRepository.sumNetBalanceCents();
    }
}
