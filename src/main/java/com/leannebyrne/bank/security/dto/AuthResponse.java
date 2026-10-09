package com.leannebyrne.bank.security.dto;

import com.leannebyrne.bank.customer.Role;
import java.util.UUID;

public record AuthResponse(String token, UUID customerId, Role role) {
}
