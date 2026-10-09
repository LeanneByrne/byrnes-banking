package com.leannebyrne.bank.security;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.leannebyrne.bank.AbstractIntegrationTest;
import com.leannebyrne.bank.security.dto.LoginRequest;
import com.leannebyrne.bank.security.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/** @Transactional rolls back the customer rows this test registers. */
@AutoConfigureMockMvc
@Transactional
class AuthControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerCreatesCustomerAndReturnsUsableToken() throws Exception {
        RegisterRequest request = new RegisterRequest("Nora", "Quinn", "nora.quinn@byrnesbanking.dev", "SecurePass123!");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.role", is("CUSTOMER")));
    }

    @Test
    void registeringWithAnAlreadyUsedEmailIsRejected() throws Exception {
        RegisterRequest request = new RegisterRequest("Alice", "Duplicate", "alice@byrnesbanking.dev", "SecurePass123!");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void loginWithSeededDemoCredentialsSucceeds() throws Exception {
        LoginRequest request = new LoginRequest("alice@byrnesbanking.dev", "Demo1234!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    void loginWithWrongPasswordIsRejected() throws Exception {
        LoginRequest request = new LoginRequest("alice@byrnesbanking.dev", "wrong-password");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
