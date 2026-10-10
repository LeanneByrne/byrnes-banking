package com.leannebyrne.bank.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.leannebyrne.bank.AbstractIntegrationTest;
import com.leannebyrne.bank.customer.Role;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * ADMIN tokens here are issued directly via JwtService for an arbitrary
 * customer id -- the filter trusts the token's role claim, so this doesn't
 * need a real seeded admin row (AdminAccountInitializer only runs when
 * ADMIN_EMAIL/ADMIN_PASSWORD are set, which they aren't in this test env).
 */
@AutoConfigureMockMvc
class SecurityIntegrationTest extends AbstractIntegrationTest {

    private static final UUID ALICE_CUSTOMER_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID BEN_CUSTOMER_ID = UUID.fromString("00000000-0000-0000-0000-000000000201");
    private static final UUID ALICE_CHECKING = UUID.fromString("00000000-0000-0000-0000-000000000111");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    private String bearer(UUID customerId, Role role) {
        return "Bearer " + jwtService.issue(customerId, role);
    }

    @Test
    void protectedRouteWithNoTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/accounts/" + ALICE_CHECKING))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedRouteWithGarbageTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/accounts/" + ALICE_CHECKING)
                        .header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void customerRoleCannotListAllCustomers() throws Exception {
        mockMvc.perform(get("/api/customers")
                        .header("Authorization", bearer(ALICE_CUSTOMER_ID, Role.CUSTOMER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminRoleCanListAllCustomers() throws Exception {
        mockMvc.perform(get("/api/customers")
                        .header("Authorization", bearer(UUID.randomUUID(), Role.ADMIN)))
                .andExpect(status().isOk());
    }

    @Test
    void customerCannotReadAnotherCustomersAccount() throws Exception {
        mockMvc.perform(get("/api/accounts/" + ALICE_CHECKING)
                        .header("Authorization", bearer(BEN_CUSTOMER_ID, Role.CUSTOMER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void ownerCanReadTheirOwnAccount() throws Exception {
        mockMvc.perform(get("/api/accounts/" + ALICE_CHECKING)
                        .header("Authorization", bearer(ALICE_CUSTOMER_ID, Role.CUSTOMER)))
                .andExpect(status().isOk());
    }

    @Test
    void customerCannotReadAnotherCustomersProfile() throws Exception {
        mockMvc.perform(get("/api/customers/" + ALICE_CUSTOMER_ID)
                        .header("Authorization", bearer(BEN_CUSTOMER_ID, Role.CUSTOMER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void ownerCanReadTheirOwnProfile() throws Exception {
        mockMvc.perform(get("/api/customers/" + ALICE_CUSTOMER_ID)
                        .header("Authorization", bearer(ALICE_CUSTOMER_ID, Role.CUSTOMER)))
                .andExpect(status().isOk());
    }

    @Test
    void customerCannotListAnotherCustomersAccounts() throws Exception {
        mockMvc.perform(get("/api/customers/" + ALICE_CUSTOMER_ID + "/accounts")
                        .header("Authorization", bearer(BEN_CUSTOMER_ID, Role.CUSTOMER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void ownerCanListTheirOwnAccounts() throws Exception {
        mockMvc.perform(get("/api/customers/" + ALICE_CUSTOMER_ID + "/accounts")
                        .header("Authorization", bearer(ALICE_CUSTOMER_ID, Role.CUSTOMER)))
                .andExpect(status().isOk());
    }

    @Test
    void actuatorHealthIsPublicWithNoToken() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void actuatorPrometheusIsPublicWithNoToken() throws Exception {
        // Prometheus's scraper can't carry a bearer token, so this endpoint is
        // deliberately exempt -- see SecurityConfig's comment on why that's an
        // acceptable tradeoff for a machine-to-machine, internal-network-only caller.
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk());
    }

    @Test
    void customerRoleCannotReadActuatorMetrics() throws Exception {
        mockMvc.perform(get("/actuator/metrics")
                        .header("Authorization", bearer(ALICE_CUSTOMER_ID, Role.CUSTOMER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void actuatorMetricsWithNoTokenIsRejected() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminRoleCanReadActuatorMetrics() throws Exception {
        mockMvc.perform(get("/actuator/metrics")
                        .header("Authorization", bearer(UUID.randomUUID(), Role.ADMIN)))
                .andExpect(status().isOk());
    }

    @Test
    void customerRoleCannotReadLedgerBalanceCheck() throws Exception {
        mockMvc.perform(get("/api/ledger/balance-check")
                        .header("Authorization", bearer(ALICE_CUSTOMER_ID, Role.CUSTOMER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminRoleCanReadLedgerBalanceCheck() throws Exception {
        mockMvc.perform(get("/api/ledger/balance-check")
                        .header("Authorization", bearer(UUID.randomUUID(), Role.ADMIN)))
                .andExpect(status().isOk());
    }
}
