package com.leannebyrne.bank.ledger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LedgerServiceTest {

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @Test
    void getNetBalanceCentsDelegatesToRepository() {
        when(ledgerEntryRepository.sumNetBalanceCents()).thenReturn(0L);
        LedgerService ledgerService = new LedgerService(ledgerEntryRepository);

        assertThat(ledgerService.getNetBalanceCents()).isZero();
    }
}
