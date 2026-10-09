package com.leannebyrne.bank.notification;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationLogRepository extends JpaRepository<NotificationLogEntry, UUID> {

    List<NotificationLogEntry> findByEventId(UUID eventId);
}
