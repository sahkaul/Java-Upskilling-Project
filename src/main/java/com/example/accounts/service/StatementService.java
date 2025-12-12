package com.example.accounts.service;

import com.example.accounts.dto.StatementDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.YearMonth;

public interface StatementService {

    /**
     * Generate statement for an account for a specific month
     */
    StatementDto generateStatement(Long accountId, YearMonth yearMonth);

    /**
     * Get statement for an account for a specific month
     */
    StatementDto getStatement(Long accountId, YearMonth yearMonth);

    /**
     * Get all statements for an account
     */
    Page<StatementDto> getAccountStatements(Long accountId, Pageable pageable);

    /**
     * Export statement to CSV format
     */
    String exportToCsv(Long accountId, YearMonth yearMonth);

    /**
     * Export statement to XLSX format
     */
    byte[] exportToXlsx(Long accountId, YearMonth yearMonth);
}

