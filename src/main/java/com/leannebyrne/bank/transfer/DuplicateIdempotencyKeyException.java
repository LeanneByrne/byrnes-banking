package com.leannebyrne.bank.transfer;

import com.leannebyrne.bank.common.BankingException;
import org.springframework.http.HttpStatus;

public class DuplicateIdempotencyKeyException extends BankingException {

    public DuplicateIdempotencyKeyException(String idempotencyKey) {
        super(HttpStatus.CONFLICT, "Idempotency-Key '" + idempotencyKey + "' was already used for a different request");
    }
}
