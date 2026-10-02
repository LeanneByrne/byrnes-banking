package com.leannebyrne.bank.customer.dto;

import com.leannebyrne.bank.customer.Customer;
import com.leannebyrne.bank.customer.Role;
import java.util.UUID;

public record CustomerResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        Role role
) {

    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getEmail(),
                customer.getRole()
        );
    }
}
