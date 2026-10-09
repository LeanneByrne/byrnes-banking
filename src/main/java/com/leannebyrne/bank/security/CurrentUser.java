package com.leannebyrne.bank.security;

import com.leannebyrne.bank.customer.Role;
import java.util.UUID;

public record CurrentUser(UUID customerId, Role role) {
}
