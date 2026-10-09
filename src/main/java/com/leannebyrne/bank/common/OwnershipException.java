package com.leannebyrne.bank.common;

import org.springframework.http.HttpStatus;

public class OwnershipException extends BankingException {

    public OwnershipException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
