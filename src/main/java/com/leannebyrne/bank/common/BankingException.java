package com.leannebyrne.bank.common;

import org.springframework.http.HttpStatus;

public abstract class BankingException extends RuntimeException {

    private final HttpStatus status;

    protected BankingException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
