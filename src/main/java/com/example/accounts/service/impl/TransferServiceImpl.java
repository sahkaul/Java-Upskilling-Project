package com.example.accounts.service.impl;

import com.example.accounts.dto.TransferRequestDto;
import com.example.accounts.dto.TransferResponseDto;
import com.example.accounts.entity.*;
import com.example.accounts.exception.*;
import com.example.accounts.reository.*;
import com.example.accounts.service.LedgerService;
import com.example.accounts.service.TransferService;
import com.example.accounts.service.IdempotencyService;
import com.example.accounts.service.AuditService;
import com.example.accounts.util.GeneratorUtil;
import com.example.accounts.util.SecurityContextUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransferServiceImpl implements TransferService {

    private final TransferRepository transferRepository;
    private final AccountRepository accountRepository;
    private final TransferHoldRepository holdRepository;
    private final TransferVersionRepository versionRepository;
    private final LedgerService ledgerService;
    private final IdempotencyService idempotencyService;
    private final AuditService auditService;

    @Value("${app.limits.customer.per-transaction:100000.00}")
    private BigDecimal customerPerTxLimit;

    @Value("${app.limits.customer.daily-aggregate:500000.00}")
    private BigDecimal customerDailyLimit;

    @Override
    @Transactional
    public TransferResponseDto initiateTransfer(TransferRequestDto request, String correlationId) {
        String idempotencyKey = request.getIdempotencyKey();

        // Check idempotency
        if (idempotencyKey != null) {
            // Generate hash using IdempotencyService
            String hash = idempotencyService.generateRequestHash(request);
            if (idempotencyService.checkIdempotency(idempotencyKey, hash, getCurrentUserId())) {
                log.info("Returning cached response for idempotency key: {}", idempotencyKey);

                // Retrieve and deserialize cached response using IdempotencyService
                TransferResponseDto cachedResponse = idempotencyService.getAndDeserializeResponse(idempotencyKey, TransferResponseDto.class);
                if (cachedResponse != null) {
                    // Update correlation ID to the new one for tracking
                    cachedResponse.setCorrelationId(correlationId);
                    return cachedResponse;
                }
            }
        }

        // Validate accounts
        Account sourceAccount = accountRepository.findById(request.getSourceAccountId())
            .orElseThrow(() -> new InvalidTransferException("Source account not found"));
        Account destinationAccount = accountRepository.findById(request.getDestinationAccountId())
            .orElseThrow(() -> new InvalidTransferException("Destination account not found"));

        // Validate account status
        if (sourceAccount.getAccountStatus() != Account.AccountStatus.ACTIVE) {
            throw new InvalidTransferException("Source account is not active");
        }
        if (destinationAccount.getAccountStatus() != Account.AccountStatus.ACTIVE) {
            throw new InvalidTransferException("Destination account is not active");
        }

        // Validate currency match (same currency for now)
        if (!sourceAccount.getCurrency().equals(destinationAccount.getCurrency())) {
            throw new InvalidTransferException("Currency mismatch between accounts");
        }

        // Validate limits
        validateTransferLimits(sourceAccount, request.getAmount());

        // Create transfer
        Transfer transfer = new Transfer();
        transfer.setSourceAccount(sourceAccount);
        transfer.setDestinationAccount(destinationAccount);
        transfer.setAmount(request.getAmount());
        transfer.setCurrency(sourceAccount.getCurrency());
        transfer.setDescription(request.getDescription());
        transfer.setTransferStatus(Transfer.TransferStatus.REQUESTED);
        transfer.setInitiatedBy(getCurrentUserId()); // Get from security context
        transfer.setIdempotencyKey(idempotencyKey);
        // currentVersion will be set automatically after saving and creating version
        transfer.setCurrentVersion(1L); // Will be reset below correctly in createTransferVersion()


        Transfer saved = transferRepository.save(transfer);

        // Create version record - this also updates/sets the currentVersion in transfer obj (via transfer Repo)
        createTransferVersion(saved, "Transfer requested");

        // Store idempotency key and response if key was provided
        if (idempotencyKey != null) {
            try {
                TransferResponseDto responseDto = convertToDto(saved, correlationId);
                idempotencyService.serializeAndStoreResponse(
                    idempotencyKey,
                    request,
                    responseDto,
                    201,  // HTTP 201 Created
                    getCurrentUserId()
                );
            } catch (Exception e) {
                log.error("Failed to store idempotency response: {}", e.getMessage());
                // Don't fail the transfer if we can't store idempotency key
                // The transfer has already been created successfully
            }
        }

        log.info("Transfer initiated. ID: {}, Amount: {}, CorrelationId: {}", saved.getTransferId(), request.getAmount(), correlationId);
        auditService.logAction(getCurrentUserId(), "TRANSFER_REQUEST", "TRANSFER", saved.getTransferId(), correlationId, "SUCCESS");

        return convertToDto(saved, correlationId);
    }

    @Override
    @Transactional
    public TransferResponseDto authorizeTransfer(Long transferId, String correlationId) {
        Transfer transfer = transferRepository.findById(transferId)
            .orElseThrow(() -> new ResourceNotFoundException("Transfer not found"));

        if (transfer.getTransferStatus() != Transfer.TransferStatus.REQUESTED) {
            throw new InvalidTransferException("Transfer is not in REQUESTED state");
        }

        transfer.setTransferStatus(Transfer.TransferStatus.AUTHORIZED);
        transfer.setAuthorizedBy(getCurrentUserId());
        transfer.setAuthorizedAt(LocalDateTime.now());

        // Create hold on source account
        TransferHold hold = new TransferHold();
        hold.setTransfer(transfer);
        hold.setAccount(transfer.getSourceAccount());
        hold.setHoldAmount(transfer.getAmount());
        hold.setReleased(false);
        holdRepository.save(hold);

        Transfer saved = transferRepository.save(transfer);
        createTransferVersion(saved, "Transfer authorized");

        log.info("Transfer authorized. ID: {}, CorrelationId: {}", transferId, correlationId);
        auditService.logAction(getCurrentUserId(), "TRANSFER_AUTHORIZE", "TRANSFER", transferId, correlationId, "SUCCESS");

        return convertToDto(saved, correlationId);
    }

    @Override
    @Transactional
    public TransferResponseDto postTransfer(Long transferId, String correlationId) {
        Transfer transfer = transferRepository.findById(transferId)
            .orElseThrow(() -> new ResourceNotFoundException("Transfer not found"));

        if (transfer.getTransferStatus() != Transfer.TransferStatus.AUTHORIZED) {
            throw new InvalidTransferException("Transfer is not in AUTHORIZED state");
        }

        // Verify funds one more time - Prevents Double Booking
        BigDecimal availableBalance = calculateAvailableBalance(transfer.getSourceAccount().getAccountId());
        if (availableBalance.compareTo(transfer.getAmount()) < 0) {
            throw new InsufficientFundsException("Insufficient funds for transfer");
        }

        // Create ledger entries
        String ledgerTxnId = GeneratorUtil.generateLedgerTransactionId();
        transfer.setLedgerTxnId(ledgerTxnId);
        ledgerService.createLedgerEntries(
            ledgerTxnId,
            transfer.getSourceAccount().getAccountId(),
            transfer.getDestinationAccount().getAccountId(),
            transfer.getAmount(),
            transfer.getDescription(),
            "TRANSFER",
            transferId
        );

        // Release holds
        List<TransferHold> holds = holdRepository.findByTransferTransferId(transferId);
        for (TransferHold hold : holds) {
            hold.setReleased(true);
            hold.setReleasedOn(System.currentTimeMillis());
            holdRepository.save(hold);
        }

        transfer.setTransferStatus(Transfer.TransferStatus.POSTED);
        transfer.setPostedAt(LocalDateTime.now());
        Transfer saved = transferRepository.save(transfer);
        createTransferVersion(saved, "Transfer posted and settled");

        log.info("Transfer posted. ID: {}, LedgerTxnId: {}, CorrelationId: {}", transferId, ledgerTxnId, correlationId);
        auditService.logAction(getCurrentUserId(), "TRANSFER_POST", "TRANSFER", transferId, correlationId, "SUCCESS");

        return convertToDto(saved, correlationId);
    }

    @Override
    @Transactional
    public TransferResponseDto cancelTransfer(Long transferId, String reason, String correlationId) {
        // Get transfer
        Transfer transfer = transferRepository.findById(transferId)
            .orElseThrow(() -> new ResourceNotFoundException("Transfer not found"));

        // Get current user from security context
        Long currentUserId = getCurrentUserId();

        // ========================================================
        // CANCELLATION POLICY ENFORCEMENT
        // ========================================================

        // POLICY 1: Cannot cancel POSTED transfers
        if (transfer.getTransferStatus() == Transfer.TransferStatus.POSTED) {
            throw new InvalidTransferException(
                "Cannot cancel a POSTED transfer. Transfers can only be cancelled before posting."
            );
        }

        // Cannot cancel already CANCELLED transfers
        if (transfer.getTransferStatus() == Transfer.TransferStatus.CANCELLED) {
            throw new InvalidTransferException(
                "Transfer is already cancelled."
            );
        }


        // POLICY 2: REQUESTED status - only requester can cancel
        if (transfer.getTransferStatus() == Transfer.TransferStatus.REQUESTED) {
            Long transferInitiator = transfer.getInitiatedBy();

            // Only the user who initiated the transfer can cancel it
            if (!transferInitiator.equals(currentUserId)) {
                log.warn("Cancellation denied: User {} attempted to cancel transfer initiated by User {}. CorrelationId: {}",
                    currentUserId, transferInitiator, correlationId);
                throw new AccessDeniedException(
                    "Only the requester can cancel a REQUESTED transfer.",
                    correlationId
                );
            }

            log.info("Transfer {} cancelled by requester (User {}). CorrelationId: {}",
                transferId, currentUserId, correlationId);
        }

        // POLICY 3: AUTHORIZED status - only BANKER/ADMIN can cancel (with reason code)
        if (transfer.getTransferStatus() == Transfer.TransferStatus.AUTHORIZED) {
            // Check if user is BANKER or ADMIN
            boolean isBanker = SecurityContextUtil.isBanker();
            boolean isAdmin = SecurityContextUtil.isAdmin();

            if (!isBanker && !isAdmin) {
                log.warn("Cancellation denied: User {} (not BANKER/ADMIN) attempted to cancel authorized transfer. CorrelationId: {}",
                    currentUserId, correlationId);
                throw new AccessDeniedException(
                    "Only BANKER or ADMIN can cancel an AUTHORIZED transfer.",
                    correlationId
                );
            }

            // Reason code is required for authorized transfers
            if (reason == null || reason.trim().isEmpty()) {
                throw new InvalidTransferException(
                    "Reason code is required to cancel an AUTHORIZED transfer."
                );
            }

            log.info("Transfer {} cancelled by {} (User {}). Reason: {}. CorrelationId: {}",
                transferId, isBanker ? "BANKER" : "ADMIN", currentUserId, reason, correlationId);
        }

        // ========================================================
        // CANCELLATION EXECUTION
        // ========================================================

        // Set status to CANCELLED
        transfer.setTransferStatus(Transfer.TransferStatus.CANCELLED);

        // Release any holds placed on accounts
        List<TransferHold> holds = holdRepository.findByTransferTransferId(transferId);
        for (TransferHold hold : holds) {
            hold.setReleased(true);
            holdRepository.save(hold);
            log.debug("Released hold on transfer {}. Hold ID: {}", transferId, hold.getHoldId());
        }

        // Save transfer
        Transfer saved = transferRepository.save(transfer);

        // Create version record documenting the cancellation
        String versionSummary = reason != null && !reason.trim().isEmpty()
            ? "Transfer cancelled: " + reason
            : "Transfer cancelled";
        createTransferVersion(saved, versionSummary);

        // Log audit action
        log.info("Transfer cancelled. ID: {}, Reason: {}, CancelledBy: {}, CorrelationId: {}",
            transferId, reason, currentUserId, correlationId);
        auditService.logAction(currentUserId, "TRANSFER_CANCEL", "TRANSFER", transferId, correlationId, "SUCCESS");

        return convertToDto(saved, correlationId);
    }

    @Override
    @Transactional(readOnly = true)
    public TransferResponseDto getTransferStatus(Long transferId) {
        Transfer transfer = transferRepository.findById(transferId)
            .orElseThrow(() -> new ResourceNotFoundException("Transfer not found"));
        return convertToDto(transfer, "");
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransferResponseDto> getTransfersByAccount(Long accountId, Pageable pageable) {
        return transferRepository.findTransfersByAccount(accountId, pageable)
            .map(t -> convertToDto(t, ""));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransferResponseDto> getTransfersWithStatus(Transfer.TransferStatus status, Pageable pageable) {
        return transferRepository.findByTransferStatus(status, pageable)
            .map(t -> convertToDto(t, ""));
    }

    // Helper methods
    private void validateTransferLimits(Account account, BigDecimal amount) {
        if (amount.compareTo(customerPerTxLimit) > 0) {
            throw new TransferLimitExceededException(
                "Per transaction limit exceeded: " + customerPerTxLimit,
                "LIMIT_PER_TX_EXCEEDED"
            );
        }

        // Check daily limit
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        List<Transfer> todaysTransfers = transferRepository.findTransfersForDailyLimit(account.getAccountId(), startOfDay);
        BigDecimal todaysTotal = todaysTransfers.stream()
            .map(Transfer::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (todaysTotal.add(amount).compareTo(customerDailyLimit) > 0) {
            throw new TransferLimitExceededException(
                "Daily limit exceeded: " + customerDailyLimit,
                "LIMIT_DAILY_EXCEEDED"
            );
        }
    }

    private BigDecimal calculateAvailableBalance(Long accountId) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        BigDecimal balance = account.getBalance();
        BigDecimal holds = holdRepository.calculateTotalHolds(accountId);
        return balance.subtract(holds);
    }

    /**
     * Create a new version of the transfer.
     * Keeps up to 10 historical versions.
     * Automatically increments version number and removes oldest versions if exceeding limit.
     *
     * @param transfer The transfer entity
     * @param changeSummary Description of what changed
     */
    private void createTransferVersion(Transfer transfer, String changeSummary) {
        // Get the current max version number for this transfer
        Long maxVersionNumber = versionRepository.getMaxVersionNumber(transfer.getTransferId());
        Long nextVersionNumber = (maxVersionNumber == null) ? 1L : maxVersionNumber + 1L;

        // Create new version record
        TransferVersion version = new TransferVersion();
        version.setTransfer(transfer);
        version.setVersionNumber(nextVersionNumber);
        version.setSourceAccountId(transfer.getSourceAccount().getAccountId());
        version.setDestinationAccountId(transfer.getDestinationAccount().getAccountId());
        version.setAmount(transfer.getAmount());
        version.setDescription(transfer.getDescription());
        version.setChangedBy(getCurrentUserId());
        version.setChangeSummary(changeSummary);

        // Save the version
        versionRepository.save(version);

        // Update transfer's currentVersion
        transfer.setCurrentVersion(nextVersionNumber);
        transferRepository.save(transfer);

        // Cleanup: Keep only last 10 versions
        List<TransferVersion> allVersions = versionRepository.findByTransfer_TransferId(transfer.getTransferId());
        if (allVersions.size() > 10) {
            // Sort by version number descending to keep the latest 10
            allVersions.sort((v1, v2) -> v2.getVersionNumber().compareTo(v1.getVersionNumber()));

            // Delete versions beyond the 10th
            List<TransferVersion> toDelete = allVersions.subList(10, allVersions.size());
            versionRepository.deleteAll(toDelete);

            log.debug("Cleaned up {} old versions for transfer {}", toDelete.size(), transfer.getTransferId());
        }

        log.debug("Created version #{} for transfer {}. Summary: {}", nextVersionNumber, transfer.getTransferId(), changeSummary);
    }

    private String generateRequestHash(TransferRequestDto request) {
        try {
            String data = request.getSourceAccountId() + "|" + request.getDestinationAccountId() + "|" + request.getAmount();
            return GeneratorUtil.hashRequest(data);
        } catch (Exception e) {
            return "";
        }
    }

    private TransferResponseDto convertToDto(Transfer transfer, String correlationId) {
        return new TransferResponseDto(
            transfer.getTransferId(),
            transfer.getSourceAccount().getAccountId(),
            transfer.getDestinationAccount().getAccountId(),
            transfer.getAmount(),
            transfer.getCurrency(),
            transfer.getTransferStatus().toString(),
            transfer.getDescription(),
            transfer.getCreatedAt(),
            transfer.getAuthorizedAt(),
            transfer.getPostedAt(),
            transfer.getIdempotencyKey(),
            correlationId
        );
    }

    /**
     * Get current userId from SecurityContext.
     * Extracted from JWT token via SecurityContextUtil.
     *
     * @return userId of currently authenticated user
     */
    private Long getCurrentUserId() {
        try {
            var currentUser = SecurityContextUtil.getCurrentUserContext();
            return currentUser.getUserId();
        } catch (Exception e) {
            log.warn("Failed to get userId from SecurityContext: {}", e.getMessage());
            // Fallback - this shouldn't happen in production
            return null;
        }
    }

    // ========================================================
    // Version Management APIs
    // ========================================================

    @Override
    @Transactional(readOnly = true)
    public List<TransferVersion> getTransferVersionHistory(Long transferId) {
        // Verify transfer exists
        transferRepository.findById(transferId)
            .orElseThrow(() -> new ResourceNotFoundException("Transfer not found"));

        // Get all versions for this transfer, sorted by version number descending (most recent first)
        List<TransferVersion> versions = versionRepository.findByTransfer_TransferId(transferId);
        versions.sort((v1, v2) -> v2.getVersionNumber().compareTo(v1.getVersionNumber()));

        log.debug("Retrieved {} versions for transfer {}", versions.size(), transferId);
        return versions;
    }

    @Override
    @Transactional
    public TransferResponseDto revertTransferVersion(Long transferId, Long versionId, String correlationId) {
        // Get transfer
        Transfer transfer = transferRepository.findById(transferId)
            .orElseThrow(() -> new ResourceNotFoundException("Transfer not found"));

        // Check if transfer can be reverted (not POSTED)
        if (transfer.getTransferStatus() == Transfer.TransferStatus.POSTED) {
            throw new InvalidTransferException("Cannot revert a POSTED transfer");
        }

        // Get the version to revert to
        TransferVersion versionToRevert = versionRepository.findById(versionId)
            .orElseThrow(() -> new ResourceNotFoundException("Transfer version not found"));

        // Verify it belongs to this transfer
        if (!versionToRevert.getTransfer().getTransferId().equals(transferId)) {
            throw new InvalidTransferException("Version does not belong to this transfer");
        }

        // Update transfer with version details
        Account sourceAccount = accountRepository.findById(versionToRevert.getSourceAccountId())
            .orElseThrow(() -> new ResourceNotFoundException("Source account not found"));
        Account destinationAccount = accountRepository.findById(versionToRevert.getDestinationAccountId())
            .orElseThrow(() -> new ResourceNotFoundException("Destination account not found"));

        transfer.setSourceAccount(sourceAccount);
        transfer.setDestinationAccount(destinationAccount);
        transfer.setAmount(versionToRevert.getAmount());
        transfer.setDescription(versionToRevert.getDescription());

        // Update initiatedBy to current user who is performing the revert
        // This tracks who initiated/performed the revert action for audit purposes
        transfer.setInitiatedBy(getCurrentUserId());

        Transfer saved = transferRepository.save(transfer);

        // Create a new version record documenting the revert
        createTransferVersion(saved, "Reverted to version #" + versionToRevert.getVersionNumber());

        log.info("Transfer {} reverted to version {}. CorrelationId: {}", transferId, versionId, correlationId);
        auditService.logAction(getCurrentUserId(), "UPDATE", "TRANSFER", transferId, correlationId, "SUCCESS");

        return convertToDto(saved, correlationId);
    }
}

