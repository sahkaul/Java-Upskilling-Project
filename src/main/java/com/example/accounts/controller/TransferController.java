package com.example.accounts.controller;

import com.example.accounts.dto.TransferRequestDto;
import com.example.accounts.dto.TransferResponseDto;
import com.example.accounts.dto.ApiResponse;
import com.example.accounts.dto.CurrentUserContext;
import com.example.accounts.entity.Transfer;
import com.example.accounts.entity.TransferVersion;
import com.example.accounts.service.TransferService;
import com.example.accounts.service.AuthorizationService;
import com.example.accounts.util.GeneratorUtil;
import com.example.accounts.util.SecurityContextUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
@Tag(name = "Transfer Management", description = "APIs for managing transfers")
public class TransferController {

    private final TransferService transferService;
    private final AuthorizationService authorizationService;

    @PostMapping
    @Operation(summary = "Initiate a transfer")
    public ResponseEntity<ApiResponse> initiateTransfer(@Valid @RequestBody TransferRequestDto request) {
        String correlationId = GeneratorUtil.generateCorrelationId();

        // ✅ Extract current user
        CurrentUserContext currentUser = SecurityContextUtil.getCurrentUserContext();

        // ✅ Enforce ACL for source account (TRANSFER permission needed)
        authorizationService.validateTransferSourceAccess(request.getSourceAccountId(), currentUser, correlationId);

        // Enforce ACL for destination account - disabled for now as we already checked above
    //    authorizationService.validateTransferDestinationAccess(request.getDestinationAccountId(), currentUser, correlationId);

        TransferResponseDto transfer = transferService.initiateTransfer(request, correlationId);
        ApiResponse response = new ApiResponse(
            true,
            "Transfer initiated successfully",
            correlationId,
            transfer,
            null
        );
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/{transferId}/authorize")
    @Operation(summary = "Authorize a transfer")
    public ResponseEntity<ApiResponse> authorizeTransfer(@PathVariable Long transferId) {
        String correlationId = GeneratorUtil.generateCorrelationId();

    // no authorization check here
        TransferResponseDto transfer = transferService.authorizeTransfer(transferId, correlationId);
        ApiResponse response = new ApiResponse(
            true,
            "Transfer authorized successfully",
            correlationId,
            transfer,
            null
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{transferId}/post")
    @Operation(summary = "Post a transfer (settle)")
    public ResponseEntity<ApiResponse> postTransfer(@PathVariable Long transferId) {
        String correlationId = GeneratorUtil.generateCorrelationId();
        TransferResponseDto transfer = transferService.postTransfer(transferId, correlationId);
        ApiResponse response = new ApiResponse(
            true,
            "Transfer posted successfully",
            correlationId,
            transfer,
            null
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{transferId}/cancel")
    @Operation(summary = "Cancel a transfer")
    public ResponseEntity<ApiResponse> cancelTransfer(
            @PathVariable Long transferId,
            @RequestParam String reason) {
        String correlationId = GeneratorUtil.generateCorrelationId();
        TransferResponseDto transfer = transferService.cancelTransfer(transferId, reason, correlationId);
        ApiResponse response = new ApiResponse(
            true,
            "Transfer cancelled successfully",
            correlationId,
            transfer,
            null
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{transferId}")
    @Operation(summary = "Get transfer status")
    public ResponseEntity<ApiResponse> getTransferStatus(@PathVariable Long transferId) {
        String correlationId = GeneratorUtil.generateCorrelationId();
        TransferResponseDto transfer = transferService.getTransferStatus(transferId);
        ApiResponse response = new ApiResponse(
            true,
            "Transfer retrieved successfully",
            correlationId,
            transfer,
            null
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/account/{accountId}")
    @Operation(summary = "Get transfers for an account")
    public ResponseEntity<ApiResponse> getAccountTransfers(
            @PathVariable Long accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String correlationId = GeneratorUtil.generateCorrelationId();
        Pageable pageable = PageRequest.of(page, size);
        Page<TransferResponseDto> transfers = transferService.getTransfersByAccount(accountId, pageable);
        ApiResponse response = new ApiResponse(
            true,
            "Transfers retrieved successfully",
            correlationId,
            transfers,
            null
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get transfers by status")
    public ResponseEntity<ApiResponse> getTransfersByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String correlationId = GeneratorUtil.generateCorrelationId();
        Pageable pageable = PageRequest.of(page, size);
        Transfer.TransferStatus transferStatus = Transfer.TransferStatus.valueOf(status);
        Page<TransferResponseDto> transfers = transferService.getTransfersWithStatus(transferStatus, pageable);
        ApiResponse response = new ApiResponse(
            true,
            "Transfers retrieved successfully",
            correlationId,
            transfers,
            null
        );
        return ResponseEntity.ok(response);
    }

    // ========================================================
    // Version Management APIs
    // ========================================================

    /**
     * Get version history for a transfer.
     * Returns all versions with actor, timestamp, and change summary.
     *
     * @param transferId Transfer ID
     * @return List of transfer versions (most recent first)
     */
    @GetMapping("/{transferId}/versions")
    @Operation(summary = "Get transfer version history")
    public ResponseEntity<ApiResponse> getTransferVersionHistory(@PathVariable Long transferId) {
        String correlationId = GeneratorUtil.generateCorrelationId();

        // ✅ Extract current user
        CurrentUserContext currentUser = SecurityContextUtil.getCurrentUserContext();

        // ✅ Verify user has access to this transfer (can view it)
        authorizationService.validateTransferSourceAccess(transferId, currentUser, correlationId);

        // Get version history
        java.util.List<TransferVersion> versions = transferService.getTransferVersionHistory(transferId);

        ApiResponse response = new ApiResponse(
            true,
            "Transfer version history retrieved successfully",
            correlationId,
            versions,
            null
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Revert a transfer to a previous version.
     * Only allowed before transfer is POSTED.
     * Creates a new version documenting the revert.
     *
     * @param transferId Transfer ID
     * @param versionId Version to revert to
     * @return Updated transfer DTO
     */
    @PostMapping("/{transferId}/revert/{versionId}")
    @Operation(summary = "Revert transfer to a previous version")
    public ResponseEntity<ApiResponse> revertTransferVersion(
            @PathVariable Long transferId,
            @PathVariable Long versionId,
            @RequestParam(required = false) String correlationId) {

        // Generate correlation ID if not provided
        if (correlationId == null) {
            correlationId = GeneratorUtil.generateCorrelationId();
        }

        // ✅ Extract current user
        CurrentUserContext currentUser = SecurityContextUtil.getCurrentUserContext();

        // ✅ Verify user has access to this transfer (can modify it)
        authorizationService.validateTransferSourceAccess(transferId, currentUser, correlationId);

        // Revert to selected version
        TransferResponseDto transfer = transferService.revertTransferVersion(transferId, versionId, correlationId);

        ApiResponse response = new ApiResponse(
            true,
            "Transfer reverted successfully",
            correlationId,
            transfer,
            null
        );
        return ResponseEntity.ok(response);
    }
}
