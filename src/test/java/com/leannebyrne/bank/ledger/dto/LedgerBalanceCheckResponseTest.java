package com.leannebyrne.bank.ledger.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LedgerBalanceCheckResponseTest {

    @Test
    void zeroNetIsBalanced() {
        assertThat(LedgerBalanceCheckResponse.of(0L).balanced()).isTrue();
    }

    @Test
    void nonZeroNetIsNotBalanced() {
        LedgerBalanceCheckResponse response = LedgerBalanceCheckResponse.of(500L);
        assertThat(response.balanced()).isFalse();
        assertThat(response.netCents()).isEqualTo(500L);
    }
}
