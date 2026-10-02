package com.leannebyrne.bank.customer;

import com.leannebyrne.bank.account.AccountService;
import com.leannebyrne.bank.account.dto.AccountResponse;
import com.leannebyrne.bank.customer.dto.CustomerResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * No auth yet (Phase 4): {@code {customerId}} is an explicit path id rather
 * than resolved from a JWT principal via a {@code /me} route.
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

    @GetMapping("/{customerId}")
    public CustomerResponse getCustomer(@PathVariable UUID customerId) {
        return CustomerResponse.from(customerService.getById(customerId));
    }

    @GetMapping("/{customerId}/accounts")
    public List<AccountResponse> getCustomerAccounts(@PathVariable UUID customerId) {
        customerService.getById(customerId);
        return accountService.getByCustomerId(customerId).stream()
                .map(account -> AccountResponse.from(account, accountService.getBalanceCents(account.getId())))
                .toList();
    }
}
