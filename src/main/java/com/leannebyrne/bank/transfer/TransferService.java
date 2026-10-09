package com.leannebyrne.bank.transfer;

import com.leannebyrne.bank.account.Account;
import com.leannebyrne.bank.account.AccountNotActiveException;
import com.leannebyrne.bank.account.AccountNotFoundException;
import com.leannebyrne.bank.account.AccountRepository;
import com.leannebyrne.bank.account.AccountService;
import com.leannebyrne.bank.account.AccountStatus;
import com.leannebyrne.bank.account.AccountType;
import com.leannebyrne.bank.common.OwnershipException;
import com.leannebyrne.bank.customer.Role;
import com.leannebyrne.bank.ledger.LedgerService;
import com.leannebyrne.bank.outbox.OutboxService;
import com.leannebyrne.bank.transfer.dto.TransferRequest;
import java.time.Instant;
import java.util.UUID;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferService {

    private final TransferRepository transferRepository;
    private final AccountRepository accountRepository;
    private final AccountService accountService;
    private final LedgerService ledgerService;
    private final OutboxService outboxService;

    /** Package-private (not final) so tests can point it at the instance under test. */
    TransferService self;

    public TransferService(
            TransferRepository transferRepository,
            AccountRepository accountRepository,
            AccountService accountService,
            LedgerService ledgerService,
            OutboxService outboxService,
            @Lazy TransferService self) {
        this.transferRepository = transferRepository;
        this.accountRepository = accountRepository;
        this.accountService = accountService;
        this.ledgerService = ledgerService;
        this.outboxService = outboxService;
        this.self = self;
    }

    /**
     * Not @Transactional itself. The idempotency lookup is a single repository
     * call (no transaction needed), and the locked write happens in
     * {@link #transferWithLocks} via the {@code self} proxy so a unique-key
     * race can be caught here and retried in a fresh transaction, rather than
     * continuing to use a Hibernate session after a failed flush.
     */
    public Transfer createTransfer(TransferRequest request, String idempotencyKey, UUID actingCustomerId,
            Role actingRole) {
        var existing = transferRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return replayOrConflict(existing.get(), request, actingCustomerId, actingRole);
        }

        try {
            return self.transferWithLocks(request, idempotencyKey, actingCustomerId, actingRole);
        } catch (DataIntegrityViolationException raceLoss) {
            // Someone else won the race on this idempotency key between our lookup
            // and our insert. Re-query in a clean transaction and return their
            // result instead of surfacing an error for a request that, from the
            // client's point of view, actually succeeded.
            Transfer winner = transferRepository.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> raceLoss);
            return replayOrConflict(winner, request, actingCustomerId, actingRole);
        }
    }

    /**
     * noRollbackFor is required: on the insufficient-funds path we deliberately
     * save a FAILED Transfer row and then throw, and without it Spring would
     * roll back that save along with everything else when the exception
     * propagates.
     */
    @Transactional(noRollbackFor = InsufficientFundsException.class)
    public Transfer transferWithLocks(TransferRequest request, String idempotencyKey, UUID actingCustomerId,
            Role actingRole) {
        UUID sourceId = request.sourceAccountId();
        UUID destinationId = request.destinationAccountId();

        if (sourceId.equals(destinationId)) {
            throw new InvalidTransferException("Source and destination accounts must differ");
        }

        // Lock both accounts in a fixed, direction-independent order (lowest id
        // first) so two opposite-direction transfers between the same two
        // accounts can never each hold one lock while waiting on the other.
        UUID firstId = sourceId.compareTo(destinationId) <= 0 ? sourceId : destinationId;
        UUID secondId = sourceId.compareTo(destinationId) <= 0 ? destinationId : sourceId;

        Account first = accountRepository.findByIdForUpdate(firstId)
                .orElseThrow(() -> new AccountNotFoundException(firstId));
        Account second = accountRepository.findByIdForUpdate(secondId)
                .orElseThrow(() -> new AccountNotFoundException(secondId));

        Account source = sourceId.equals(firstId) ? first : second;
        Account destination = sourceId.equals(firstId) ? second : first;

        // A customer may only initiate a transfer FROM an account they own.
        // Receiving a transfer from someone else is normal, so the destination
        // has no such restriction.
        requireOwnership(source.getCustomerId(), actingCustomerId, actingRole);

        requireActive(source);
        requireActive(destination);

        if (!source.getCurrency().equals(destination.getCurrency())) {
            throw new InvalidTransferException("Source and destination accounts use different currencies");
        }

        long amountCents = request.amountCents();

        // The SYSTEM account is the exogenous funding source seeded in Phase 1
        // and is expected to run a large negative derived balance; every other
        // account type must stay non-negative.
        boolean sourceMayGoNegative = source.getAccountType() == AccountType.SYSTEM;
        if (!sourceMayGoNegative) {
            long sourceBalanceCents = accountService.getBalanceCents(source.getId());
            if (sourceBalanceCents < amountCents) {
                Transfer failed = new Transfer(source.getId(), destination.getId(), amountCents,
                        source.getCurrency(), idempotencyKey);
                failed.markFailed("Insufficient funds");
                transferRepository.saveAndFlush(failed);
                throw new InsufficientFundsException(source.getId());
            }
        }

        // Flushed immediately (not just saved) so a unique-key race on
        // idempotency_key surfaces here, before any ledger rows are written.
        Transfer transfer = new Transfer(source.getId(), destination.getId(), amountCents,
                source.getCurrency(), idempotencyKey);
        transfer = transferRepository.saveAndFlush(transfer);

        ledgerService.recordTransfer(transfer.getId(), source.getId(), destination.getId(), amountCents);
        transfer.markCompleted();

        // Written in this same transaction as the ledger entries above, not
        // after it commits -- see ADR 0008. If this transaction rolls back,
        // the event never existed either; there is no window where one
        // happened without the other.
        outboxService.recordEvent(TransferCompletedEvent.EVENT_TYPE, new TransferCompletedEvent(
                transfer.getId(), source.getId(), source.getCustomerId(),
                destination.getId(), destination.getCustomerId(),
                amountCents, transfer.getCurrency(), Instant.now()));

        return transfer;
    }

    private void requireActive(Account account) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountNotActiveException(account.getId(), account.getStatus());
        }
    }

    private void requireOwnership(UUID resourceCustomerId, UUID actingCustomerId, Role actingRole) {
        if (actingRole != Role.ADMIN && !resourceCustomerId.equals(actingCustomerId)) {
            throw new OwnershipException("You do not have access to this account");
        }
    }

    /**
     * A replay returns a previously-computed result without re-running any
     * business logic, but it still needs its own ownership check: without
     * this, a different customer who happened to submit the exact same
     * Idempotency-Key and payload as someone else's successful transfer could
     * fetch back that transfer's details.
     */
    private Transfer replayOrConflict(Transfer existing, TransferRequest request, UUID actingCustomerId,
            Role actingRole) {
        if (!matchesRequest(existing, request)) {
            throw new DuplicateIdempotencyKeyException(existing.getIdempotencyKey());
        }

        Account source = accountRepository.findById(existing.getSourceAccountId())
                .orElseThrow(() -> new AccountNotFoundException(existing.getSourceAccountId()));
        requireOwnership(source.getCustomerId(), actingCustomerId, actingRole);

        if (existing.getStatus() == TransferStatus.FAILED) {
            // Only failure mode today is insufficient funds, so replaying a
            // known-failed transfer re-throws the same error every time rather
            // than re-running the check against a balance that may have changed.
            throw new InsufficientFundsException(existing.getSourceAccountId());
        }
        return existing;
    }

    private boolean matchesRequest(Transfer existing, TransferRequest request) {
        return existing.getSourceAccountId().equals(request.sourceAccountId())
                && existing.getDestinationAccountId().equals(request.destinationAccountId())
                && existing.getAmountCents() == request.amountCents();
    }
}
