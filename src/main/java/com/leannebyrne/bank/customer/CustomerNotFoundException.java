package com.leannebyrne.bank.customer;

import com.leannebyrne.bank.common.BankingException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class CustomerNotFoundException extends BankingException {

    public CustomerNotFoundException(UUID customerId) {
        super(HttpStatus.NOT_FOUND, "Customer not found: " + customerId);
    }
}
