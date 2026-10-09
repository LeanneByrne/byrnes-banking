package com.leannebyrne.bank.customer;

import com.leannebyrne.bank.common.OwnershipException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional(readOnly = true)
    public Customer getById(UUID customerId, UUID actingCustomerId, Role actingRole) {
        if (actingRole != Role.ADMIN && !customerId.equals(actingCustomerId)) {
            throw new OwnershipException("You do not have access to this customer");
        }
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));
    }

    /** ADMIN-only at the route level (see SecurityConfig) — no ownership check needed here. */
    @Transactional(readOnly = true)
    public List<Customer> getAll() {
        return customerRepository.findAll();
    }
}
