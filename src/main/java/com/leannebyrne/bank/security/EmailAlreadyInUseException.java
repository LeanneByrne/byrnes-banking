package com.leannebyrne.bank.security;

import com.leannebyrne.bank.common.BankingException;
import org.springframework.http.HttpStatus;

public class EmailAlreadyInUseException extends BankingException {

    public EmailAlreadyInUseException(String email) {
        super(HttpStatus.CONFLICT, "Email '" + email + "' is already registered");
    }
}
