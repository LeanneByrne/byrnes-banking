package com.leannebyrne.bank.ledger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.leannebyrne.bank.AbstractIntegrationTest;
import com.leannebyrne.bank.customer.Role;
import com.leannebyrne.bank.security.JwtService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class LedgerControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Test
    void seededLedgerNetsToZero() throws Exception {
        String adminToken = "Bearer " + jwtService.issue(UUID.randomUUID(), Role.ADMIN);

        mockMvc.perform(get("/api/ledger/balance-check").header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.netCents").value(0))
                .andExpect(jsonPath("$.balanced").value(true));
    }
}
