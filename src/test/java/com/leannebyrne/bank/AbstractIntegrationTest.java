package com.leannebyrne.bank;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Manual "singleton container" pattern: the container is started once in a
 * static initializer and never stopped by us (Ryuk reaps it at JVM exit).
 * Deliberately NOT using {@code @Testcontainers}/{@code @Container} — with
 * Testcontainers 2.0.5, that JUnit5 lifecycle extension does not recognize an
 * already-started static container when a second {@code @SpringBootTest}
 * subclass runs, starts a redundant second container, and leaves the
 * already-cached Spring context's connection pool pointing at the first
 * (now-orphaned) one. Starting it manually and relying solely on
 * {@code @ServiceConnection} for wiring sidesteps that entirely.
 */
@Tag("integration")
@SpringBootTest
public abstract class AbstractIntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }
}
