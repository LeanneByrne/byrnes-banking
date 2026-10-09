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
}
