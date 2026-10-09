package com.leannebyrne.bank.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    private final String secret;
    private final long expirationMinutes;

    public JwtProperties(String secret, long expirationMinutes) {
        this.secret = secret;
        this.expirationMinutes = expirationMinutes;
    }

    public String getSecret() {
        return secret;
    }

    public long getExpirationMinutes() {
        return expirationMinutes;
    }
}
