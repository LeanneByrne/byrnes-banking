package com.leannebyrne.bank.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.leannebyrne.bank.common.OwnershipException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    private CustomerService customerService;

    @Test
    void getByIdReturnsCustomerWhenOwnerAsks() {
        customerService = new CustomerService(customerRepository);
        Customer customer = new Customer("Alice", "Nguyen", "alice@byrnesbanking.dev", "hash", Role.CUSTOMER);
        UUID customerId = UUID.randomUUID();
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));

        Customer result = customerService.getById(customerId, customerId, Role.CUSTOMER);

        assertThat(result).isSameAs(customer);
    }

    @Test
    void getByIdThrowsWhenNotFound() {
        customerService = new CustomerService(customerRepository);
        UUID customerId = UUID.randomUUID();
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.getById(customerId, customerId, Role.CUSTOMER))
                .isInstanceOf(CustomerNotFoundException.class);
    }

    @Test
    void getByIdThrowsOwnershipExceptionForADifferentCustomer() {
        customerService = new CustomerService(customerRepository);
        UUID customerId = UUID.randomUUID();

        assertThatThrownBy(() -> customerService.getById(customerId, UUID.randomUUID(), Role.CUSTOMER))
                .isInstanceOf(OwnershipException.class);
    }

    @Test
    void getByIdAllowsAdminRegardlessOfOwnership() {
        customerService = new CustomerService(customerRepository);
        Customer customer = new Customer("Alice", "Nguyen", "alice@byrnesbanking.dev", "hash", Role.CUSTOMER);
        UUID customerId = UUID.randomUUID();
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));

        Customer result = customerService.getById(customerId, UUID.randomUUID(), Role.ADMIN);

        assertThat(result).isSameAs(customer);
    }

    @Test
    void getAllReturnsEveryCustomer() {
        customerService = new CustomerService(customerRepository);
        Customer customer = new Customer("Ben", "Carter", "ben@byrnesbanking.dev", "hash", Role.CUSTOMER);
        when(customerRepository.findAll()).thenReturn(List.of(customer));

        List<Customer> result = customerService.getAll();

        assertThat(result).containsExactly(customer);
    }
}
