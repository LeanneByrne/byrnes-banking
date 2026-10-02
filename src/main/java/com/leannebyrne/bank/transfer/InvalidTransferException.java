package com.leannebyrne.bank.transfer;

import com.leannebyrne.bank.common.BankingException;
import org.springframework.http.HttpStatus;

public class InvalidTransferException extends BankingException {

    public InvalidTransferException(String reason) {
        super(HttpStatus.BAD_REQUEST, reason);
    }
}
