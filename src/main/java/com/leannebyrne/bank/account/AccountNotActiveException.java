package com.leannebyrne.bank.account;

import com.leannebyrne.bank.common.BankingException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class AccountNotActiveException extends BankingException {

    public AccountNotActiveException(UUID accountId, AccountStatus status) {
        super(HttpStatus.CONFLICT, "Account " + accountId + " is not active (status: " + status + ")");
    }
}
