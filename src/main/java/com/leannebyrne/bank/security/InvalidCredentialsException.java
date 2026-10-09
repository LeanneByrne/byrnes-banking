package com.leannebyrne.bank.security;

import com.leannebyrne.bank.common.BankingException;
import org.springframework.http.HttpStatus;

public class InvalidCredentialsException extends BankingException {

    public InvalidCredentialsException() {
        super(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }
}
