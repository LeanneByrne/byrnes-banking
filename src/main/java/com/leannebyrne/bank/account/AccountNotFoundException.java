package com.leannebyrne.bank.account;

import com.leannebyrne.bank.common.BankingException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class AccountNotFoundException extends BankingException {

    public AccountNotFoundException(UUID accountId) {
        super(HttpStatus.NOT_FOUND, "Account not found: " + accountId);
    }
}
