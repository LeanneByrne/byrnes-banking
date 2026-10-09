package com.leannebyrne.bank.transfer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.leannebyrne.bank.AbstractIntegrationTest;
import com.leannebyrne.bank.account.Account;
import com.leannebyrne.bank.account.AccountRepository;
import com.leannebyrne.bank.account.AccountType;
import com.leannebyrne.bank.customer.Customer;
import com.leannebyrne.bank.customer.CustomerRepository;
import com.leannebyrne.bank.customer.Role;
import com.leannebyrne.bank.ledger.EntryType;
import com.leannebyrne.bank.ledger.LedgerEntryRepository;
import com.leannebyrne.bank.security.JwtService;
import com.leannebyrne.bank.transfer.dto.TransferRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

/**
 * Deliberately NOT @Transactional (unlike the other integration tests in
 * this package) -- a real concurrency proof needs each simulated request to
 * run in its own transaction against the real Testcontainers Postgres, not
 * share one test-rollback transaction across threads. That means this test's
 * writes persist in the shared container for the rest of the run, which is
 * why it creates entirely fresh customers/accounts rather than touching the
 * seeded Alice/Ben/Priya data other tests assert fixed values against.
 */
@AutoConfigureMockMvc
class TransferConcurrencyIntegrationTest extends AbstractIntegrationTest {

    private static final int CONCURRENT_REQUESTS = 20;
    private static final long STARTING_BALANCE_CENTS = 10_000L;
    private static final long WITHDRAWAL_AMOUNT_CENTS = 1_000L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @Autowired
    private JwtService jwtService;

    private Account createAccountFor(Customer customer, AccountType type) {
        String accountNumber = "CT" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        return accountRepository.save(new Account(customer.getId(), accountNumber, type, "USD"));
    }

    private String bearer(UUID customerId) {
        return "Bearer " + jwtService.issue(customerId, Role.CUSTOMER);
    }

    private long wholeLedgerNetBalance() {
        return ledgerEntryRepository.findAll().stream()
                .mapToLong(e -> e.getEntryType() == EntryType.CREDIT ? e.getAmountCents() : -e.getAmountCents())
                .sum();
    }

    @Test
    void manySimultaneousTransfersNeverOverdrawTheSourceAccount() throws Exception {
        Customer sourceOwner = customerRepository.save(
                new Customer("Concurrency", "Source", "concurrency-source-" + UUID.randomUUID() + "@test.local",
                        "unused-hash", Role.CUSTOMER));
        Customer sinkOwner = customerRepository.save(
                new Customer("Concurrency", "Sink", "concurrency-sink-" + UUID.randomUUID() + "@test.local",
                        "unused-hash", Role.CUSTOMER));
        // A fresh SYSTEM-type account of its own, owned by sourceOwner --
        // deliberately NOT the real seeded SYSTEM account, since funding from
        // that one would change its balance and break
        // AccountBalanceIntegrationTest's fixed assertion for it. Any account
        // can carry AccountType.SYSTEM; TransferService's insufficient-funds
        // exemption checks the type, not a specific id.
        Account fundingSource = createAccountFor(sourceOwner, AccountType.SYSTEM);
        Account source = createAccountFor(sourceOwner, AccountType.CHECKING);
        Account sink = createAccountFor(sinkOwner, AccountType.CHECKING);

        // sourceOwner owns both the funding account and the real source
        // account, so this is just an ordinary transfer -- no ADMIN bypass needed.
        fundAccount(fundingSource.getId(), source.getId(), sourceOwner.getId(), STARTING_BALANCE_CENTS);

        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_REQUESTS);
        List<Callable<Integer>> attempts = new ArrayList<>();
        for (int i = 0; i < CONCURRENT_REQUESTS; i++) {
            String idempotencyKey = "concurrency-attempt-" + i + "-" + UUID.randomUUID();
            attempts.add(() -> attemptTransfer(source.getId(), sink.getId(), sourceOwner.getId(), idempotencyKey));
        }

        List<Future<Integer>> results = executor.invokeAll(attempts, 30, TimeUnit.SECONDS);
        executor.shutdown();

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger insufficientFundsCount = new AtomicInteger();
        for (Future<Integer> result : results) {
            int status = result.get();
            if (status == 201) {
                successCount.incrementAndGet();
            } else if (status == 422) {
                insufficientFundsCount.incrementAndGet();
            }
        }

        int expectedSuccesses = (int) (STARTING_BALANCE_CENTS / WITHDRAWAL_AMOUNT_CENTS);
        assertThat(successCount.get()).isEqualTo(expectedSuccesses);
        assertThat(insufficientFundsCount.get()).isEqualTo(CONCURRENT_REQUESTS - expectedSuccesses);

        long finalBalance = sumLedger(source.getId());
        assertThat(finalBalance).isZero();
        assertThat(finalBalance).isNotNegative();
        assertThat(wholeLedgerNetBalance()).isZero();
    }

    private void fundAccount(UUID fundingSourceId, UUID accountId, UUID ownerId, long amountCents) throws Exception {
        String body = objectMapper.writeValueAsString(new TransferRequest(fundingSourceId, accountId, amountCents));
        int status = mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(ownerId))
                        .header("Idempotency-Key", "concurrency-funding-" + UUID.randomUUID())
                        .content(body))
                .andReturn().getResponse().getStatus();
        assertThat(status).isEqualTo(201);
    }

    private int attemptTransfer(UUID sourceId, UUID sinkId, UUID ownerId, String idempotencyKey) throws Exception {
        String body = objectMapper.writeValueAsString(new TransferRequest(sourceId, sinkId, WITHDRAWAL_AMOUNT_CENTS));
        return mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(ownerId))
                        .header("Idempotency-Key", idempotencyKey)
                        .content(body))
                .andReturn().getResponse().getStatus();
    }

    private long sumLedger(UUID accountId) {
        return ledgerEntryRepository.findAll().stream()
                .filter(e -> e.getAccountId().equals(accountId))
                .mapToLong(e -> e.getEntryType() == EntryType.CREDIT ? e.getAmountCents() : -e.getAmountCents())
                .sum();
    }
}
