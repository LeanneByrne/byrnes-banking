package com.leannebyrne.bank.transfer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.leannebyrne.bank.AbstractIntegrationTest;
import com.leannebyrne.bank.customer.Role;
import com.leannebyrne.bank.ledger.EntryType;
import com.leannebyrne.bank.ledger.LedgerEntryRepository;
import com.leannebyrne.bank.security.JwtService;
import com.leannebyrne.bank.transfer.dto.TransferRequest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * {@code @Transactional} here rolls each test method back at the end, since
 * these tests write real transfers against the same shared Testcontainers
 * Postgres that other integration tests (e.g. AccountBalanceIntegrationTest)
 * assert fixed seeded balances against. MockMvc dispatches in-process on the
 * test thread (no separate server thread), so this rollback-per-test pattern
 * applies cleanly.
 *
 * Every request now needs a real JWT -- issued directly via JwtService for
 * the seeded demo customer who owns the source account, rather than mocking
 * Spring Security's context, so these tests exercise the real
 * JwtAuthenticationFilter end to end.
 */
@AutoConfigureMockMvc
@Transactional
class TransferIntegrationTest extends AbstractIntegrationTest {

    private static final UUID ALICE_CUSTOMER_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID PRIYA_CUSTOMER_ID = UUID.fromString("00000000-0000-0000-0000-000000000301");
    private static final UUID ALICE_CHECKING = UUID.fromString("00000000-0000-0000-0000-000000000111");
    private static final UUID BEN_CHECKING = UUID.fromString("00000000-0000-0000-0000-000000000211");
    private static final UUID PRIYA_CHECKING = UUID.fromString("00000000-0000-0000-0000-000000000311");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @Autowired
    private JwtService jwtService;

    private String bearer(UUID customerId) {
        return "Bearer " + jwtService.issue(customerId, Role.CUSTOMER);
    }

    private long wholeLedgerNetBalance() {
        return ledgerEntryRepository.findAll().stream()
                .mapToLong(e -> e.getEntryType() == EntryType.CREDIT ? e.getAmountCents() : -e.getAmountCents())
                .sum();
    }

    @Test
    void normalTransferMovesMoneyAndKeepsTheLedgerBalanced() throws Exception {
        TransferRequest request = new TransferRequest(ALICE_CHECKING, BEN_CHECKING, 10_000L);

        mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(ALICE_CUSTOMER_ID))
                        .header("Idempotency-Key", "it-normal-transfer")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("COMPLETED")));

        assertThat(wholeLedgerNetBalance()).isZero();
    }

    @Test
    void repeatedIdempotencyKeyDoesNotDoubleApply() throws Exception {
        TransferRequest request = new TransferRequest(ALICE_CHECKING, BEN_CHECKING, 7_500L);
        String body = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(ALICE_CUSTOMER_ID))
                        .header("Idempotency-Key", "it-repeated-key")
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists());

        String firstTransferId = mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(ALICE_CUSTOMER_ID))
                        .header("Idempotency-Key", "it-repeated-key")
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        assertThat(firstTransferId).contains("COMPLETED");
        assertThat(wholeLedgerNetBalance()).isZero();
    }

    @Test
    void overdraftAttemptFailsAndRetryingReturnsTheSameFailure() throws Exception {
        TransferRequest request = new TransferRequest(PRIYA_CHECKING, BEN_CHECKING, 999_999_999L);
        String body = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(PRIYA_CUSTOMER_ID))
                        .header("Idempotency-Key", "it-overdraft")
                        .content(body))
                .andExpect(status().isUnprocessableEntity());

        // Retrying the exact same request/key must fail the same way, not succeed.
        mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(PRIYA_CUSTOMER_ID))
                        .header("Idempotency-Key", "it-overdraft")
                        .content(body))
                .andExpect(status().isUnprocessableEntity());

        assertThat(wholeLedgerNetBalance()).isZero();
    }

    @Test
    void missingIdempotencyKeyHeaderIsRejected() throws Exception {
        TransferRequest request = new TransferRequest(ALICE_CHECKING, BEN_CHECKING, 1_000L);

        mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(ALICE_CUSTOMER_ID))
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingAuthorizationHeaderIsRejected() throws Exception {
        TransferRequest request = new TransferRequest(ALICE_CHECKING, BEN_CHECKING, 1_000L);

        mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "it-no-auth")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void nonOwningCustomerCannotInitiateTransferFromAnothersAccount() throws Exception {
        // Priya's token, but the source account belongs to Alice.
        TransferRequest request = new TransferRequest(ALICE_CHECKING, BEN_CHECKING, 1_000L);

        mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(PRIYA_CUSTOMER_ID))
                        .header("Idempotency-Key", "it-ownership")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        assertThat(wholeLedgerNetBalance()).isZero();
    }
}
