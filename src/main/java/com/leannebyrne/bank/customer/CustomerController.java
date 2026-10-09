package com.leannebyrne.bank.customer;

import com.leannebyrne.bank.account.AccountService;
import com.leannebyrne.bank.account.dto.AccountResponse;
import com.leannebyrne.bank.customer.dto.CustomerResponse;
import com.leannebyrne.bank.security.CurrentUser;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code {customerId}} stays an explicit path id rather than a {@code /me}
 * route; ownership is enforced in CustomerService/AccountService. GET
 * /api/customers (list all) is ADMIN-only at the route level (SecurityConfig).
 */
@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;
    private final AccountService accountService;

    public CustomerController(CustomerService customerService, AccountService accountService) {
        this.customerService = customerService;
        this.accountService = accountService;
    }

    @GetMapping
    public List<CustomerResponse> getAllCustomers() {
        return customerService.getAll().stream().map(CustomerResponse::from).toList();
    }

    @GetMapping("/{customerId}")
    public CustomerResponse getCustomer(@PathVariable UUID customerId, @AuthenticationPrincipal CurrentUser currentUser) {
        return CustomerResponse.from(customerService.getById(customerId, currentUser.customerId(), currentUser.role()));
    }

    @GetMapping("/{customerId}/accounts")
    public List<AccountResponse> getCustomerAccounts(@PathVariable UUID customerId,
            @AuthenticationPrincipal CurrentUser currentUser) {
        customerService.getById(customerId, currentUser.customerId(), currentUser.role());
        return accountService.getByCustomerId(customerId, currentUser.customerId(), currentUser.role()).stream()
                .map(account -> AccountResponse.from(account, accountService.getBalanceCents(account.getId())))
                .toList();
    }
}
