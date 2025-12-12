package com.example.accounts.controller;

import com.example.accounts.dto.BankerDto;
import com.example.accounts.dto.AccountsDto;
import com.example.accounts.dto.CreateAccountForCustomerRequestDto;
import com.example.accounts.dto.ApiResponse;
import com.example.accounts.dto.CurrentUserContext;
import com.example.accounts.service.BankerService;
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

/**
 * REST Controller for Banker Management
 *
 * Endpoints for:
 * - Creating and managing bankers
 * - Assigning/unassigning customers and accounts
 * - Managing account status (freeze/unfreeze/close)
 *
 * Requires ADMIN or BANKER role (with appropriate scoping)
 */
@RestController
@RequestMapping("/api/bankers")
@RequiredArgsConstructor
@Tag(name = "Banker Management", description = "APIs for managing bankers and their assignments")
public class BankerController {

    private final BankerService bankerService;
    private final AuthorizationService authorizationService;

    /**
     * Create a new banker
     *
     * Note: userId is extracted from JWT token automatically
     */
    @PostMapping
    @Operation(summary = "Create a new banker")
    public ResponseEntity<ApiResponse> createBanker(@Valid @RequestBody BankerDto bankerDto) {
        String correlationId = GeneratorUtil.generateCorrelationId();

        // ✅ Extract userId from JWT token
        Long userId = SecurityContextUtil.getCurrentUserContext().getUserId();
        bankerDto.setUserId(userId);

        BankerDto created = bankerService.createBanker(bankerDto);
        ApiResponse response = new ApiResponse(
            true,
            "Banker created successfully",
            correlationId,
            created,
            null
        );
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Get banker by ID
     */
    @GetMapping("/{bankerId}")
    @Operation(summary = "Get banker by ID")
    public ResponseEntity<ApiResponse> getBanker(@PathVariable Long bankerId) {
        String correlationId = GeneratorUtil.generateCorrelationId();
        BankerDto banker = bankerService.getBankerById(bankerId);
        ApiResponse response = new ApiResponse(
            true,
            "Banker retrieved successfully",
            correlationId,
            banker,
            null
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Get banker by user ID
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get banker by user ID")
    public ResponseEntity<ApiResponse> getBankerByUserId(@PathVariable Long userId) {
        String correlationId = GeneratorUtil.generateCorrelationId();
        BankerDto banker = bankerService.getBankerByUserId(userId);
        ApiResponse response = new ApiResponse(
            true,
            "Banker retrieved successfully",
            correlationId,
            banker,
            null
        );
        return ResponseEntity.ok(response);
    }


    /**
     * Update banker information
     */
    @PutMapping("/{bankerId}")
    @Operation(summary = "Update banker information")
    public ResponseEntity<ApiResponse> updateBanker(
            @PathVariable Long bankerId,
            @Valid @RequestBody BankerDto bankerDto) {
        String correlationId = GeneratorUtil.generateCorrelationId();
        BankerDto updated = bankerService.updateBanker(bankerId, bankerDto);
        ApiResponse response = new ApiResponse(
            true,
            "Banker updated successfully",
            correlationId,
            updated,
            null
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Deactivate banker
     */
    @DeleteMapping("/{bankerId}")
    @Operation(summary = "Deactivate banker")
    public ResponseEntity<ApiResponse> deactivateBanker(@PathVariable Long bankerId) {
        String correlationId = GeneratorUtil.generateCorrelationId();
        bankerService.deactivateBanker(bankerId);
        ApiResponse response = new ApiResponse(
            true,
            "Banker deactivated successfully",
            correlationId,
            null,
            null
        );
        return ResponseEntity.ok(response);
    }


    // Assignment Management APIs

    /**
     * Assign customer to banker
     *
     * Admin only operation
     */
    @PostMapping("/assign-customer")
    @Operation(summary = "Assign customer to banker (Admin only)")
    public ResponseEntity<ApiResponse> assignCustomer(
            @RequestParam Long bankerId,
            @RequestParam Long customerId) {
        String correlationId = GeneratorUtil.generateCorrelationId();

        // ✅ Extract current user from JWT
        CurrentUserContext currentUser = SecurityContextUtil.getCurrentUserContext();

        // ✅ Validate that current user is ADMIN
        authorizationService.validateAdminAccess(currentUser, correlationId);

        // ✅ Assign customer to banker
        bankerService.assignCustomerToBanker(bankerId, customerId, correlationId);

        ApiResponse response = new ApiResponse(
            true,
            "Customer assigned to banker successfully",
            correlationId,
            null,
            null
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Unassign customer from banker
     *
     * Admin only operation
     */
    @PostMapping("/unassign-customer")
    @Operation(summary = "Unassign customer from banker (Admin only)")
    public ResponseEntity<ApiResponse> unassignCustomer(
            @RequestParam Long bankerId,
            @RequestParam Long customerId) {
        String correlationId = GeneratorUtil.generateCorrelationId();

        // ✅ Extract current user from JWT
        CurrentUserContext currentUser = SecurityContextUtil.getCurrentUserContext();

        // ✅ Validate that current user is ADMIN
        authorizationService.validateAdminAccess(currentUser, correlationId);

        // ✅ Unassign customer from banker (with bankerId validation)
        bankerService.unassignCustomerFromBanker(bankerId, customerId, correlationId);

        ApiResponse response = new ApiResponse(
            true,
            "Customer unassigned from banker successfully",
            correlationId,
            null,
            null
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Assign account to banker
     *
     * Admin only operation
     */
    @PostMapping("/assign-account")
    @Operation(summary = "Assign account to banker (Admin only)")
    public ResponseEntity<ApiResponse> assignAccount(
            @RequestParam Long bankerId,
            @RequestParam Long accountId) {
        String correlationId = GeneratorUtil.generateCorrelationId();

        // ✅ Extract current user from JWT
        CurrentUserContext currentUser = SecurityContextUtil.getCurrentUserContext();

        // ✅ Validate that current user is ADMIN
        authorizationService.validateAdminAccess(currentUser, correlationId);

        // ✅ Assign account to banker
        bankerService.assignAccountToBanker(bankerId, accountId, correlationId);

        ApiResponse response = new ApiResponse(
            true,
            "Account assigned to banker successfully",
            correlationId,
            null,
            null
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Unassign account from banker
     *
     * Admin only operation
     */
    @PostMapping("/unassign-account")
    @Operation(summary = "Unassign account from banker (Admin only)")
    public ResponseEntity<ApiResponse> unassignAccount(
            @RequestParam Long bankerId,
            @RequestParam Long accountId) {
        String correlationId = GeneratorUtil.generateCorrelationId();

        // ✅ Extract current user from JWT
        CurrentUserContext currentUser = SecurityContextUtil.getCurrentUserContext();

        // ✅ Validate that current user is ADMIN
        authorizationService.validateAdminAccess(currentUser, correlationId);

        // ✅ Unassign account from banker (with bankerId validation)
        bankerService.unassignAccountFromBanker(bankerId, accountId, correlationId);

        ApiResponse response = new ApiResponse(
            true,
            "Account unassigned from banker successfully",
            correlationId,
            null,
            null
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Get assigned customers for banker
     */
    @GetMapping("/{bankerId}/customers")
    @Operation(summary = "Get customers assigned to banker")
    public ResponseEntity<ApiResponse> getAssignedCustomers(
            @PathVariable Long bankerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String correlationId = GeneratorUtil.generateCorrelationId();
        Pageable pageable = PageRequest.of(page, size);
        Page<Long> customerIds = bankerService.getAssignedCustomers(bankerId, pageable);
        ApiResponse response = new ApiResponse(
            true,
            "Assigned customers retrieved successfully",
            correlationId,
            customerIds,
            null
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Get assigned accounts for banker
     */
    @GetMapping("/{bankerId}/accounts")
    @Operation(summary = "Get accounts assigned to banker")
    public ResponseEntity<ApiResponse> getAssignedAccounts(
            @PathVariable Long bankerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String correlationId = GeneratorUtil.generateCorrelationId();
        Pageable pageable = PageRequest.of(page, size);
        Page<Long> accountIds = bankerService.getAssignedAccounts(bankerId, pageable);
        ApiResponse response = new ApiResponse(
            true,
            "Assigned accounts retrieved successfully",
            correlationId,
            accountIds,
            null
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Create account for assigned customer
     *
     * Banker can create an account for a customer they are assigned to.
     * BankerId is extracted from JWT token (current user).
     * Authorization validation ensures the banker is assigned to the customer.
     */
    @PostMapping("/customers/accounts")
    @Operation(summary = "Create account for assigned customer")
    public ResponseEntity<ApiResponse> createAccountForCustomer(
            @Valid @RequestBody CreateAccountForCustomerRequestDto requestDto) {
        String correlationId = GeneratorUtil.generateCorrelationId();

        // ✅ Extract current user from JWT token
        CurrentUserContext currentUser = SecurityContextUtil.getCurrentUserContext();

        // ✅ Validate that current user is a BANKER and has access to the customer
        // This also validates that the customer exists and banker is assigned to it
        authorizationService.validateCustomerAccess(requestDto.getCustomerId(), currentUser, correlationId);

        // Create AccountsDto from request
        AccountsDto accountsDto = new AccountsDto();
        accountsDto.setAccountType(requestDto.getAccountType());
        accountsDto.setCurrency(requestDto.getCurrency());
        accountsDto.setBranchAddress(requestDto.getBranchAddress());

        // Call BankerService to create account
        // BankerId is derived from the validated customer access
        AccountsDto createdAccount = bankerService.createAccountForCustomer(
            requestDto.getCustomerId(),
            accountsDto,
            correlationId
        );

        ApiResponse response = new ApiResponse(
            true,
            "Account created successfully for customer",
            correlationId,
            createdAccount,
            null
        );
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}

