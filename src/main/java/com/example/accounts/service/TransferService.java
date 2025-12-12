package com.example.accounts.service;

import com.example.accounts.entity.Transfer;
import com.example.accounts.dto.TransferRequestDto;
import com.example.accounts.dto.TransferResponseDto;
import com.example.accounts.entity.TransferVersion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TransferService {
    TransferResponseDto initiateTransfer(TransferRequestDto request, String correlationId);

    TransferResponseDto authorizeTransfer(Long transferId, String correlationId);

    TransferResponseDto postTransfer(Long transferId, String correlationId);

    TransferResponseDto cancelTransfer(Long transferId, String reason, String correlationId);

    TransferResponseDto getTransferStatus(Long transferId);

    Page<TransferResponseDto> getTransfersByAccount(Long accountId, Pageable pageable);

    Page<TransferResponseDto> getTransfersWithStatus(Transfer.TransferStatus status, Pageable pageable);

    // ========================================================
    // Version Management APIs
    // ========================================================

    /**
     * Get version history for a transfer.
     * Returns up to the last 10 versions with actor, timestamp, and change summary.
     *
     * @param transferId Transfer ID
     * @return List of transfer versions (most recent first)
     */
    List<TransferVersion> getTransferVersionHistory(Long transferId);

    /**
     * Revert transfer to a previous version.
     * Only allowed before transfer is POSTED.
     * Creates a new version copied from the selected prior version.
     *
     * @param transferId Transfer ID
     * @param versionId Version to revert to
     * @param correlationId Correlation ID for audit
     * @return Updated transfer DTO
     */
    TransferResponseDto revertTransferVersion(Long transferId, Long versionId, String correlationId);
}





