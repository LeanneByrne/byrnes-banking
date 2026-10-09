package com.leannebyrne.bank.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.leannebyrne.bank.customer.Role;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "a-sufficiently-long-test-secret-value-for-hs256-signing-xxxxx";

    @Test
    void issuedTokenParsesBackToTheSameCustomerIdAndRole() {
        JwtService jwtService = new JwtService(new JwtProperties(SECRET, 60));
        UUID customerId = UUID.randomUUID();

        String token = jwtService.issue(customerId, Role.ADMIN);
        Optional<CurrentUser> parsed = jwtService.tryParse(token);

        assertThat(parsed).isPresent();
        assertThat(parsed.get().customerId()).isEqualTo(customerId);
        assertThat(parsed.get().role()).isEqualTo(Role.ADMIN);
    }

    @Test
    void tokenSignedWithADifferentSecretFailsToParse() {
        JwtService issuer = new JwtService(new JwtProperties(SECRET, 60));
        JwtService verifier = new JwtService(
                new JwtProperties("a-completely-different-secret-value-xxxxxxxxxxxxxxxxxxxx", 60));

        String token = issuer.issue(UUID.randomUUID(), Role.CUSTOMER);

        assertThat(verifier.tryParse(token)).isEmpty();
    }

    @Test
    void expiredTokenFailsToParse() {
        JwtService jwtService = new JwtService(new JwtProperties(SECRET, -1));

        String token = jwtService.issue(UUID.randomUUID(), Role.CUSTOMER);

        assertThat(jwtService.tryParse(token)).isEmpty();
    }

    @Test
    void garbageTokenFailsToParse() {
        JwtService jwtService = new JwtService(new JwtProperties(SECRET, 60));

        assertThat(jwtService.tryParse("not-a-real-jwt")).isEmpty();
    }
}
