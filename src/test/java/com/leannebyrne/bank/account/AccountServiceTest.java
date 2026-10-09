package com.leannebyrne.bank.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.leannebyrne.bank.common.OwnershipException;
import com.leannebyrne.bank.customer.Role;
import com.leannebyrne.bank.ledger.LedgerEntryRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    private AccountService accountService;

    @Test
    void getByIdReturnsAccountWhenOwnerAsks() {
        accountService = new AccountService(accountRepository, ledgerEntryRepository);
        UUID customerId = UUID.randomUUID();
        Account account = new Account(customerId, "ACC1", AccountType.CHECKING, "USD");
        UUID accountId = UUID.randomUUID();
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        Account result = accountService.getById(accountId, customerId, Role.CUSTOMER);

        assertThat(result).isSameAs(account);
    }

    @Test
    void getByIdThrowsWhenNotFound() {
        accountService = new AccountService(accountRepository, ledgerEntryRepository);
        UUID accountId = UUID.randomUUID();
        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getById(accountId, UUID.randomUUID(), Role.CUSTOMER))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void getByIdThrowsOwnershipExceptionWhenNotTheOwner() {
        accountService = new AccountService(accountRepository, ledgerEntryRepository);
        Account account = new Account(UUID.randomUUID(), "ACC1", AccountType.CHECKING, "USD");
        UUID accountId = UUID.randomUUID();
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountService.getById(accountId, UUID.randomUUID(), Role.CUSTOMER))
                .isInstanceOf(OwnershipException.class);
    }

    @Test
    void getByIdAllowsAdminRegardlessOfOwnership() {
        accountService = new AccountService(accountRepository, ledgerEntryRepository);
        Account account = new Account(UUID.randomUUID(), "ACC1", AccountType.CHECKING, "USD");
        UUID accountId = UUID.randomUUID();
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        Account result = accountService.getById(accountId, UUID.randomUUID(), Role.ADMIN);

        assertThat(result).isSameAs(account);
    }

    @Test
    void getBalanceCentsDelegatesToLedgerAggregateQuery() {
        accountService = new AccountService(accountRepository, ledgerEntryRepository);
        UUID accountId = UUID.randomUUID();
        when(ledgerEntryRepository.sumBalanceCentsByAccountId(accountId)).thenReturn(12_345L);

        long balance = accountService.getBalanceCents(accountId);

        assertThat(balance).isEqualTo(12_345L);
    }

    @Test
    void getByCustomerIdReturnsAllAccountsForThatCustomer() {
        accountService = new AccountService(accountRepository, ledgerEntryRepository);
        UUID customerId = UUID.randomUUID();
        Account account = new Account(customerId, "ACC1", AccountType.SAVINGS, "USD");
        when(accountRepository.findByCustomerId(customerId)).thenReturn(List.of(account));

        List<Account> result = accountService.getByCustomerId(customerId, customerId, Role.CUSTOMER);

        assertThat(result).containsExactly(account);
    }

    @Test
    void getByCustomerIdThrowsOwnershipExceptionForADifferentCustomer() {
        accountService = new AccountService(accountRepository, ledgerEntryRepository);
        UUID customerId = UUID.randomUUID();

        assertThatThrownBy(() -> accountService.getByCustomerId(customerId, UUID.randomUUID(), Role.CUSTOMER))
                .isInstanceOf(OwnershipException.class);
    }
}
