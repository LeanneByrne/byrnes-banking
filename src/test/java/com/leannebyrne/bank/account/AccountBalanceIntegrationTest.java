package com.leannebyrne.bank.account;

import static org.assertj.core.api.Assertions.assertThat;

import com.leannebyrne.bank.AbstractIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Asserts derived balances against the known, fixed amounts in Flyway's
 * V2__seed_demo_data.sql (6 funding transfers from the SYSTEM account + 1
 * peer-to-peer demo transfer — see that file for the arithmetic).
 */
class AccountBalanceIntegrationTest extends AbstractIntegrationTest {

    private static final UUID ALICE_CHECKING = UUID.fromString("00000000-0000-0000-0000-000000000111");
    private static final UUID ALICE_SAVINGS = UUID.fromString("00000000-0000-0000-0000-000000000112");
    private static final UUID BEN_CHECKING = UUID.fromString("00000000-0000-0000-0000-000000000211");
    private static final UUID BEN_SAVINGS = UUID.fromString("00000000-0000-0000-0000-000000000212");
    private static final UUID PRIYA_CHECKING = UUID.fromString("00000000-0000-0000-0000-000000000311");
    private static final UUID PRIYA_SAVINGS = UUID.fromString("00000000-0000-0000-0000-000000000312");
    private static final UUID SYSTEM_ACCOUNT = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID ALICE_CUSTOMER_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Test
    void derivesBalancesFromSeededLedgerEntries() {
        assertThat(accountService.getBalanceCents(ALICE_CHECKING)).isEqualTo(245_000L);
        assertThat(accountService.getBalanceCents(ALICE_SAVINGS)).isEqualTo(1_000_000L);
        assertThat(accountService.getBalanceCents(BEN_CHECKING)).isEqualTo(125_050L);
        assertThat(accountService.getBalanceCents(BEN_SAVINGS)).isEqualTo(500_000L);
        assertThat(accountService.getBalanceCents(PRIYA_CHECKING)).isEqualTo(80_000L);
        assertThat(accountService.getBalanceCents(PRIYA_SAVINGS)).isEqualTo(1_525_075L);
        assertThat(accountService.getBalanceCents(SYSTEM_ACCOUNT)).isEqualTo(-3_475_125L);
    }

    @Test
    void accountWithNoLedgerEntriesDerivesToZero() {
        String accountNumber = "TST" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        Account freshAccount = accountRepository.save(
                new Account(ALICE_CUSTOMER_ID, accountNumber, AccountType.CHECKING, "USD"));

        assertThat(accountService.getBalanceCents(freshAccount.getId())).isZero();
    }
}
