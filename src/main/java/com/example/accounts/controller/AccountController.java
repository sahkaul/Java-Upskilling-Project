package com.example.accounts.controller;

import com.example.accounts.dto.AccountsDto;
import com.example.accounts.dto.ApiResponse;
import com.example.accounts.dto.CurrentUserContext;
import com.example.accounts.dto.StatementDto;
import com.example.accounts.service.AccountService;
import com.example.accounts.service.AuthorizationService;
import com.example.accounts.service.StatementService;
import com.example.accounts.util.GeneratorUtil;
import com.example.accounts.util.SecurityContextUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.YearMonth;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@Tag(name = "Account Management", description = "APIs for managing accounts")
public class AccountController {

    private final AccountService accountService;
    private final AuthorizationService authorizationService;
    private final StatementService statementService;

    @PostMapping
    @Operation(summary = "Create a new account")
    public ResponseEntity<ApiResponse> createAccount(
            @Valid @RequestBody AccountsDto accountsDto,
            @RequestParam Long customerId) {
        String correlationId = GeneratorUtil.generateCorrelationId();
        AccountsDto created = accountService.createAccount(accountsDto, customerId);
        ApiResponse response = new ApiResponse(
            true,
            "Account created successfully",
            correlationId,
            created,
            null
        );
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{accountId}")
    @Operation(summary = "Get account by ID")
    public ResponseEntity<ApiResponse> getAccount(@PathVariable Long accountId) {
        String correlationId = GeneratorUtil.generateCorrelationId();

        // ✅ Step 1: Extract current user from JWT
        CurrentUserContext currentUser = SecurityContextUtil.getCurrentUserContext();

        // ✅ Step 2: Enforce ACL - Check if user can view this account
        authorizationService.validateAccountViewAccess(accountId, currentUser, correlationId);

        // ✅ Step 3: If authorization passed, proceed with business logic
        AccountsDto account = accountService.getAccountById(accountId);
        ApiResponse response = new ApiResponse(
            true,
            "Account retrieved successfully",
            correlationId,
            account,
            null
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/number/{accountNumber}")
    @Operation(summary = "Get account by account number")
    public ResponseEntity<ApiResponse> getAccountByNumber(@PathVariable String accountNumber) {
        String correlationId = GeneratorUtil.generateCorrelationId();

        // ✅ Step 1: Extract current user from JWT
        CurrentUserContext currentUser = SecurityContextUtil.getCurrentUserContext();

        // ✅ Step 2: Get the account first to get its ID for ACL check
        AccountsDto accountDto = accountService.getAccountByNumber(accountNumber);

        // ✅ Step 3: Enforce ACL - Check if user can view this account
        authorizationService.validateAccountViewAccess(accountDto.getAccountId(), currentUser, correlationId);

        ApiResponse response = new ApiResponse(
            true,
            "Account retrieved successfully",
            correlationId,
            accountDto,
            null
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get all accounts for a customer")
    public ResponseEntity<ApiResponse> getCustomerAccounts(
            @PathVariable Long customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String correlationId = GeneratorUtil.generateCorrelationId();
        Pageable pageable = PageRequest.of(page, size);
        Page<AccountsDto> accounts = accountService.getCustomerAccounts(customerId, pageable);
        ApiResponse response = new ApiResponse(
            true,
            "Accounts retrieved successfully",
            correlationId,
            accounts,
            null
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Get all accounts")
    public ResponseEntity<ApiResponse> getAllAccounts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String correlationId = GeneratorUtil.generateCorrelationId();
        Pageable pageable = PageRequest.of(page, size);
        Page<AccountsDto> accounts = accountService.getAllAccounts(pageable);
        ApiResponse response = new ApiResponse(
            true,
            "Accounts retrieved successfully",
            correlationId,
            accounts,
            null
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{accountId}/balance")
    @Operation(summary = "Get account balance")
    public ResponseEntity<ApiResponse> getBalance(@PathVariable Long accountId) {
        String correlationId = GeneratorUtil.generateCorrelationId();

        // ✅ Extract current user and enforce ACL
        CurrentUserContext currentUser = SecurityContextUtil.getCurrentUserContext();
        authorizationService.validateAccountViewAccess(accountId, currentUser, correlationId);

        BigDecimal balance = accountService.getAccountBalance(accountId);
        ApiResponse response = new ApiResponse(
            true,
            "Balance retrieved successfully",
            correlationId,
            balance,
            null
        );
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{accountId}")
    @Operation(summary = "Update account")
    public ResponseEntity<ApiResponse> updateAccount(
            @PathVariable Long accountId,
            @Valid @RequestBody AccountsDto accountsDto) {
        String correlationId = GeneratorUtil.generateCorrelationId();

        // ✅ Extract current user and enforce ACL for UPDATE
        CurrentUserContext currentUser = SecurityContextUtil.getCurrentUserContext();
        authorizationService.validateAccountUpdateAccess(accountId, currentUser, correlationId);

        AccountsDto updated = accountService.updateAccount(accountId, accountsDto);
        ApiResponse response = new ApiResponse(
            true,
            "Account updated successfully",
            correlationId,
            updated,
            null
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{accountId}/freeze")
    @Operation(summary = "Freeze account")
    public ResponseEntity<ApiResponse> freezeAccount(
            @PathVariable Long accountId,
            @RequestParam String reason) {
        String correlationId = GeneratorUtil.generateCorrelationId();

        // ✅ Extract current user and enforce ACL for UPDATE
        CurrentUserContext currentUser = SecurityContextUtil.getCurrentUserContext();
        authorizationService.validateAccountUpdateAccess(accountId, currentUser, correlationId);

        accountService.freezeAccount(accountId, reason);
        ApiResponse response = new ApiResponse(
            true,
            "Account frozen successfully",
            correlationId,
            null,
            null
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{accountId}/unfreeze")
    @Operation(summary = "Unfreeze account")
    public ResponseEntity<ApiResponse> unfreezeAccount(@PathVariable Long accountId) {
        String correlationId = GeneratorUtil.generateCorrelationId();

        // ✅ Extract current user and enforce ACL for UPDATE
        CurrentUserContext currentUser = SecurityContextUtil.getCurrentUserContext();
        authorizationService.validateAccountUpdateAccess(accountId, currentUser, correlationId);

        accountService.unfreezeAccount(accountId);
        ApiResponse response = new ApiResponse(
            true,
            "Account unfrozen successfully",
            correlationId,
            null,
            null
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{accountId}/close")
    @Operation(summary = "Close account")
    public ResponseEntity<ApiResponse> closeAccount(
            @PathVariable Long accountId,
            @RequestParam String reason) {
        String correlationId = GeneratorUtil.generateCorrelationId();

        // ✅ Extract current user and enforce ACL for DELETE
        CurrentUserContext currentUser = SecurityContextUtil.getCurrentUserContext();
        authorizationService.validateAccountDeleteAccess(accountId, currentUser, correlationId);

        accountService.closeAccount(accountId, reason);
        ApiResponse response = new ApiResponse(
            true,
            "Account closed successfully",
            correlationId,
            null,
            null
        );
        return ResponseEntity.ok(response);
    }

    // ============= STATEMENT ENDPOINTS =============

    @GetMapping("/{accountId}/statement")
    @Operation(summary = "Get account statement for a specific month")
    public ResponseEntity<ApiResponse> getStatement(
            @PathVariable Long accountId,
            @RequestParam(required = false) String yearMonth) {
        String correlationId = GeneratorUtil.generateCorrelationId();


     //   CurrentUserContext currentUser = SecurityContextUtil.getCurrentUserContext();
       // authorizationService.validateAccountViewAccess(accountId, currentUser, correlationId);

        // Parse year-month or use current month
        YearMonth period = yearMonth != null ? YearMonth.parse(yearMonth) : YearMonth.now();

        StatementDto statement = statementService.getStatement(accountId, period);
        ApiResponse response = new ApiResponse(
            true,
            "Statement retrieved successfully",
            correlationId,
            statement,
            null
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{accountId}/statement/generate")
    @Operation(summary = "Generate account statement for a specific month")
    public ResponseEntity<ApiResponse> generateStatement(
            @PathVariable Long accountId,
            @RequestParam(required = false) String yearMonth) {
        String correlationId = GeneratorUtil.generateCorrelationId();

        // ✅ Extract current user and enforce ACL
     //   CurrentUserContext currentUser = SecurityContextUtil.getCurrentUserContext();
       // authorizationService.validateAccountViewAccess(accountId, currentUser, correlationId);

        // Parse year-month or use current month
        YearMonth period = yearMonth != null ? YearMonth.parse(yearMonth) : YearMonth.now();

        StatementDto statement = statementService.generateStatement(accountId, period);
        ApiResponse response = new ApiResponse(
            true,
            "Statement generated successfully",
            correlationId,
            statement,
            null
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{accountId}/statements")
    @Operation(summary = "Get all statements for an account")
    public ResponseEntity<ApiResponse> getAccountStatements(
            @PathVariable Long accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        String correlationId = GeneratorUtil.generateCorrelationId();

        // ✅ Extract current user and enforce ACL
    //    CurrentUserContext currentUser = SecurityContextUtil.getCurrentUserContext();
      //  authorizationService.validateAccountViewAccess(accountId, currentUser, correlationId);

        Pageable pageable = PageRequest.of(page, size);
        Page<StatementDto> statements = statementService.getAccountStatements(accountId, pageable);
        ApiResponse response = new ApiResponse(
            true,
            "Statements retrieved successfully",
            correlationId,
            statements,
            null
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{accountId}/statement/download/csv")
    @Operation(summary = "Download statement as CSV file")
    public ResponseEntity<String> downloadStatementCsv(
            @PathVariable Long accountId,
            @RequestParam(required = false) String yearMonth) {

        // ✅ Extract current user and enforce ACL
      //  CurrentUserContext currentUser = SecurityContextUtil.getCurrentUserContext();
      //  authorizationService.validateAccountViewAccess(accountId, currentUser, "");

        // Parse year-month or use current month
        YearMonth period = yearMonth != null ? YearMonth.parse(yearMonth) : YearMonth.now();

        String csvContent = statementService.exportToCsv(accountId, period);

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=statement_" + accountId + "_" + period + ".csv")
                .body(csvContent);
    }

    @GetMapping("/{accountId}/statement/download/xlsx")
    @Operation(summary = "Download statement as XLSX file")
    public ResponseEntity<byte[]> downloadStatementXlsx(
            @PathVariable Long accountId,
            @RequestParam(required = false) String yearMonth) {

        // ✅ Extract current user and enforce ACL
      //  CurrentUserContext currentUser = SecurityContextUtil.getCurrentUserContext();
       // authorizationService.validateAccountViewAccess(accountId, currentUser, "");

        // Parse year-month or use current month
        YearMonth period = yearMonth != null ? YearMonth.parse(yearMonth) : YearMonth.now();

        byte[] xlsxContent = statementService.exportToXlsx(accountId, period);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=statement_" + accountId + "_" + period + ".xlsx")
                .body(xlsxContent);
    }
}

