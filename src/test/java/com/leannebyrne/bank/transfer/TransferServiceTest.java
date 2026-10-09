package com.leannebyrne.bank.transfer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.leannebyrne.bank.account.Account;
import com.leannebyrne.bank.account.AccountNotActiveException;
import com.leannebyrne.bank.account.AccountRepository;
import com.leannebyrne.bank.account.AccountService;
import com.leannebyrne.bank.account.AccountStatus;
import com.leannebyrne.bank.account.AccountType;
import com.leannebyrne.bank.ledger.LedgerService;
import com.leannebyrne.bank.transfer.dto.TransferRequest;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    // Fixed so UUID ordering between them is deterministic across test runs.
    private static final UUID LOWER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID HIGHER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountService accountService;

    @Mock
    private LedgerService ledgerService;

    private TransferService transferService;

    @BeforeEach
    void setUp() {
        transferService = new TransferService(transferRepository, accountRepository, accountService, ledgerService, null);
        // Production wiring does this via a @Lazy Spring proxy; tests have no
        // Spring context, so point self at the instance under test directly.
        transferService.self = transferService;
    }

    private static Account account(UUID id, AccountType type, AccountStatus status, String currency) {
        Account account = new Account(UUID.randomUUID(), "ACC-" + id, type, currency);
        ReflectionTestUtils.setField(account, "id", id);
        ReflectionTestUtils.setField(account, "status", status);
        return account;
    }

    private void stubBothAccountsFound(Account lower, Account higher) {
        when(accountRepository.findByIdForUpdate(LOWER_ID)).thenReturn(Optional.of(lower));
        when(accountRepository.findByIdForUpdate(HIGHER_ID)).thenReturn(Optional.of(higher));
    }

    @Test
    void transferDebitsSourceAndCreditsDestinationForExactAmount() {
        Account source = account(LOWER_ID, AccountType.CHECKING, AccountStatus.ACTIVE, "USD");
        Account destination = account(HIGHER_ID, AccountType.CHECKING, AccountStatus.ACTIVE, "USD");
        stubBothAccountsFound(source, destination);
        when(accountService.getBalanceCents(LOWER_ID)).thenReturn(10_000L);
        when(transferRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TransferRequest request = new TransferRequest(LOWER_ID, HIGHER_ID, 5_000L);
        Transfer result = transferService.createTransfer(request, "key-1");

        assertThat(result.getStatus()).isEqualTo(TransferStatus.COMPLETED);
        verify(ledgerService).recordTransfer(any(), eq(LOWER_ID), eq(HIGHER_ID), eq(5_000L));
    }

    @Test
    void insufficientFundsPersistsFailedTransferAndThrows() {
        Account source = account(LOWER_ID, AccountType.CHECKING, AccountStatus.ACTIVE, "USD");
        Account destination = account(HIGHER_ID, AccountType.CHECKING, AccountStatus.ACTIVE, "USD");
        stubBothAccountsFound(source, destination);
        when(accountService.getBalanceCents(LOWER_ID)).thenReturn(1_000L);
        when(transferRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TransferRequest request = new TransferRequest(LOWER_ID, HIGHER_ID, 5_000L);

        assertThatThrownBy(() -> transferService.createTransfer(request, "key-2"))
                .isInstanceOf(InsufficientFundsException.class);

        verify(transferRepository).saveAndFlush(
                Mockito.argThat(t -> t.getStatus() == TransferStatus.FAILED));
        verify(ledgerService, never()).recordTransfer(any(), any(), any(), any(Long.class));
    }

    @Test
    void systemAccountSourceSkipsInsufficientFundsCheck() {
        Account systemAccount = account(LOWER_ID, AccountType.SYSTEM, AccountStatus.ACTIVE, "USD");
        Account destination = account(HIGHER_ID, AccountType.CHECKING, AccountStatus.ACTIVE, "USD");
        stubBothAccountsFound(systemAccount, destination);
        when(transferRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TransferRequest request = new TransferRequest(LOWER_ID, HIGHER_ID, 1_000_000L);
        Transfer result = transferService.createTransfer(request, "key-3");

        assertThat(result.getStatus()).isEqualTo(TransferStatus.COMPLETED);
        verify(accountService, never()).getBalanceCents(any());
        verify(ledgerService).recordTransfer(any(), eq(LOWER_ID), eq(HIGHER_ID), eq(1_000_000L));
    }

    @Test
    void sameAccountTransferRejectedBeforeAnyLockingAttempt() {
        TransferRequest request = new TransferRequest(LOWER_ID, LOWER_ID, 100L);

        assertThatThrownBy(() -> transferService.createTransfer(request, "key-4"))
                .isInstanceOf(InvalidTransferException.class);

        verify(accountRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void inactiveDestinationAccountRejected() {
        Account source = account(LOWER_ID, AccountType.CHECKING, AccountStatus.ACTIVE, "USD");
        Account destination = account(HIGHER_ID, AccountType.CHECKING, AccountStatus.FROZEN, "USD");
        stubBothAccountsFound(source, destination);

        TransferRequest request = new TransferRequest(LOWER_ID, HIGHER_ID, 100L);

        assertThatThrownBy(() -> transferService.createTransfer(request, "key-5"))
                .isInstanceOf(AccountNotActiveException.class);
    }

    @Test
    void currencyMismatchRejected() {
        Account source = account(LOWER_ID, AccountType.CHECKING, AccountStatus.ACTIVE, "USD");
        Account destination = account(HIGHER_ID, AccountType.CHECKING, AccountStatus.ACTIVE, "EUR");
        stubBothAccountsFound(source, destination);

        TransferRequest request = new TransferRequest(LOWER_ID, HIGHER_ID, 100L);

        assertThatThrownBy(() -> transferService.createTransfer(request, "key-6"))
                .isInstanceOf(InvalidTransferException.class);
    }

    @Test
    void locksLowerAccountIdFirstRegardlessOfTransferDirection() {
        Account low = account(LOWER_ID, AccountType.CHECKING, AccountStatus.ACTIVE, "USD");
        Account high = account(HIGHER_ID, AccountType.CHECKING, AccountStatus.ACTIVE, "USD");
        stubBothAccountsFound(low, high);
        when(accountService.getBalanceCents(any())).thenReturn(10_000L);
        when(transferRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Money moving HIGHER -> LOWER should still lock LOWER first.
        TransferRequest request = new TransferRequest(HIGHER_ID, LOWER_ID, 100L);
        transferService.createTransfer(request, "key-7");

        InOrder inOrder = Mockito.inOrder(accountRepository);
        inOrder.verify(accountRepository).findByIdForUpdate(LOWER_ID);
        inOrder.verify(accountRepository).findByIdForUpdate(HIGHER_ID);
    }

    @Test
    void idempotentReplayOfCompletedTransferReturnsOriginalWithoutNewLocks() {
        Transfer existing = new Transfer(LOWER_ID, HIGHER_ID, 500L, "USD", "key-8");
        existing.markCompleted();
        when(transferRepository.findByIdempotencyKey("key-8")).thenReturn(Optional.of(existing));

        TransferRequest request = new TransferRequest(LOWER_ID, HIGHER_ID, 500L);
        Transfer result = transferService.createTransfer(request, "key-8");

        assertThat(result).isSameAs(existing);
        verify(accountRepository, never()).findByIdForUpdate(any());
        verify(ledgerService, never()).recordTransfer(any(), any(), any(), any(Long.class));
    }

    @Test
    void idempotentReplayOfFailedTransferRethrowsSameError() {
        Transfer existing = new Transfer(LOWER_ID, HIGHER_ID, 500L, "USD", "key-9");
        existing.markFailed("Insufficient funds");
        when(transferRepository.findByIdempotencyKey("key-9")).thenReturn(Optional.of(existing));

        TransferRequest request = new TransferRequest(LOWER_ID, HIGHER_ID, 500L);

        assertThatThrownBy(() -> transferService.createTransfer(request, "key-9"))
                .isInstanceOf(InsufficientFundsException.class);
        verify(accountRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void reusedKeyWithDifferentPayloadThrowsDuplicateIdempotencyKeyException() {
        Transfer existing = new Transfer(LOWER_ID, HIGHER_ID, 500L, "USD", "key-10");
        existing.markCompleted();
        when(transferRepository.findByIdempotencyKey("key-10")).thenReturn(Optional.of(existing));

        TransferRequest differentAmount = new TransferRequest(LOWER_ID, HIGHER_ID, 999L);

        assertThatThrownBy(() -> transferService.createTransfer(differentAmount, "key-10"))
                .isInstanceOf(DuplicateIdempotencyKeyException.class);
    }

    @Test
    void concurrentFirstTimeRequestsOnSameKeyReturnWinnersResultInsteadOfErroring() {
        Account source = account(LOWER_ID, AccountType.CHECKING, AccountStatus.ACTIVE, "USD");
        Account destination = account(HIGHER_ID, AccountType.CHECKING, AccountStatus.ACTIVE, "USD");
        stubBothAccountsFound(source, destination);
        when(accountService.getBalanceCents(LOWER_ID)).thenReturn(10_000L);

        Transfer winner = new Transfer(LOWER_ID, HIGHER_ID, 500L, "USD", "key-11");
        winner.markCompleted();

        when(transferRepository.findByIdempotencyKey("key-11"))
                .thenReturn(Optional.empty(), Optional.of(winner));
        when(transferRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate key"));

        TransferRequest request = new TransferRequest(LOWER_ID, HIGHER_ID, 500L);
        Transfer result = transferService.createTransfer(request, "key-11");

        assertThat(result).isSameAs(winner);
    }
}
