package com.leannebyrne.bank.account.dto;

import com.leannebyrne.bank.account.Account;
import com.leannebyrne.bank.account.AccountStatus;
import com.leannebyrne.bank.account.AccountType;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        UUID customerId,
        String accountNumber,
        AccountType accountType,
        String currency,
        AccountStatus status,
        long balanceCents
) {

    public static AccountResponse from(Account account, long balanceCents) {
        return new AccountResponse(
                account.getId(),
                account.getCustomerId(),
                account.getAccountNumber(),
                account.getAccountType(),
                account.getCurrency(),
                account.getStatus(),
                balanceCents
        );
    }
}
