package com.leannebyrne.bank.outbox;

import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

/**
 * Not @Transactional itself, same as LedgerService -- it participates in
 * whatever transaction the caller already has open, which is the entire
 * point of the outbox pattern: this write has to commit atomically with the
 * business change it describes, or not at all.
 */
@Service
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OutboxService(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    public void recordEvent(String eventType, Object payload) {
        String payloadJson = objectMapper.writeValueAsString(payload);
        outboxEventRepository.save(new OutboxEvent(eventType, payloadJson));
    }
}
