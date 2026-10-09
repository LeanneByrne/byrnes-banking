package com.leannebyrne.bank.transfer;

import com.leannebyrne.bank.common.BankingException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class InsufficientFundsException extends BankingException {

    public InsufficientFundsException(UUID sourceAccountId) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "Account " + sourceAccountId + " has insufficient funds for this transfer");
    }
}
