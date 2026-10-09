package com.leannebyrne.bank.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.leannebyrne.bank.AbstractIntegrationTest;
import com.leannebyrne.bank.account.Account;
import com.leannebyrne.bank.account.AccountRepository;
import com.leannebyrne.bank.account.AccountType;
import com.leannebyrne.bank.audit.AuditLogRepository;
import com.leannebyrne.bank.customer.Customer;
import com.leannebyrne.bank.customer.CustomerRepository;
import com.leannebyrne.bank.customer.Role;
import com.leannebyrne.bank.notification.NotificationLogRepository;
import com.leannebyrne.bank.security.JwtService;
import com.leannebyrne.bank.transfer.dto.TransferRequest;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

/**
 * Deliberately NOT @Transactional, same reasoning as
 * TransferConcurrencyIntegrationTest: the outbox relay (OutboxRelay) polls
 * on its own schedule against the real Testcontainers Postgres from a
 * different thread/connection than this test, so a row written inside an
 * open, uncommitted test transaction would simply never be visible to it.
 * This test needs its writes to actually commit.
 */
@AutoConfigureMockMvc
class TransferEventPipelineIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private NotificationLogRepository notificationLogRepository;

    @Autowired
    private JwtService jwtService;

    private Account createAccountFor(Customer customer, AccountType type) {
        String accountNumber = "EV" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        return accountRepository.save(new Account(customer.getId(), accountNumber, type, "USD"));
    }

    @Test
    void completedTransferFlowsThroughOutboxKafkaAndBothConsumers() throws Exception {
        Customer sourceOwner = customerRepository.save(
                new Customer("Event", "Source", "event-source-" + UUID.randomUUID() + "@test.local",
                        "unused-hash", Role.CUSTOMER));
        Customer destinationOwner = customerRepository.save(
                new Customer("Event", "Destination", "event-destination-" + UUID.randomUUID() + "@test.local",
                        "unused-hash", Role.CUSTOMER));
        // A disposable SYSTEM-type account of its own -- same reasoning as
        // TransferConcurrencyIntegrationTest: funding from the real seeded
        // SYSTEM account would change its balance and break
        // AccountBalanceIntegrationTest's fixed assertion for it.
        Account fundingSource = createAccountFor(sourceOwner, AccountType.SYSTEM);
        Account source = createAccountFor(sourceOwner, AccountType.CHECKING);
        Account destination = createAccountFor(destinationOwner, AccountType.CHECKING);

        fund(fundingSource.getId(), source.getId(), sourceOwner.getId(), 10_000L);

        UUID transferId = transfer(source.getId(), destination.getId(), sourceOwner.getId(), 2_500L);

        OutboxEvent published = await("outbox event published to Kafka")
                .atMost(Duration.ofSeconds(10))
                .until(() -> findOutboxEventForTransfer(transferId), event -> event != null && event.getPublishedAt() != null);

        await("audit log consumer recorded the event")
                .atMost(Duration.ofSeconds(10))
                .until(() -> auditLogRepository.existsByEventId(published.getId()));

        await("notification consumer notified both customers")
                .atMost(Duration.ofSeconds(10))
                .until(() -> notificationLogRepository.findByEventId(published.getId()).size() == 2);

        List<UUID> notifiedCustomers = notificationLogRepository.findByEventId(published.getId()).stream()
                .map(n -> n.getCustomerId())
                .toList();
        assertThat(notifiedCustomers).containsExactlyInAnyOrder(sourceOwner.getId(), destinationOwner.getId());
    }

    private OutboxEvent findOutboxEventForTransfer(UUID transferId) {
        return outboxEventRepository.findAll().stream()
                .filter(e -> e.getPayload().contains(transferId.toString()))
                .findFirst()
                .orElse(null);
    }

    private String bearer(UUID customerId) {
        return "Bearer " + jwtService.issue(customerId, Role.CUSTOMER);
    }

    private void fund(UUID fundingSourceId, UUID accountId, UUID ownerId, long amountCents) throws Exception {
        transfer(fundingSourceId, accountId, ownerId, amountCents);
    }

    private UUID transfer(UUID sourceId, UUID destinationId, UUID ownerId, long amountCents) throws Exception {
        String body = objectMapper.writeValueAsString(new TransferRequest(sourceId, destinationId, amountCents));
        String response = mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(ownerId))
                        .header("Idempotency-Key", "event-pipeline-" + UUID.randomUUID())
                        .content(body))
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(response).get("id").asString());
    }
}
