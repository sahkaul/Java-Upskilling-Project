package com.example.accounts.service.impl;

import com.example.accounts.dto.StatementDto;
import com.example.accounts.dto.StatementLineItemDto;
import com.example.accounts.entity.Account;
import com.example.accounts.entity.LedgerEntry;
import com.example.accounts.reository.AccountRepository;
import com.example.accounts.reository.LedgerRepository;
import com.example.accounts.service.StatementService;
import com.opencsv.CSVWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatementServiceImpl implements StatementService {

    private final AccountRepository accountRepository;
    private final LedgerRepository ledgerRepository;

    @Override
    @Transactional(readOnly = true)
    public StatementDto generateStatement(Long accountId, YearMonth yearMonth) {
        log.info("Generating statement for account {} for period {}", accountId, yearMonth);

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        // Calculate period dates
        LocalDateTime periodStart = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime periodEnd = yearMonth.atEndOfMonth().atTime(23, 59, 59);

        // Get ledger entries for the period
        List<LedgerEntry> ledgerEntries = ledgerRepository.findLedgerByAccountAndDateRange(
                accountId, periodStart, periodEnd);

        // Calculate opening balance (balance before period start)
        LocalDateTime beforePeriodStart = periodStart.minusSeconds(1);
        BigDecimal openingBalance = calculateBalanceBeforeDate(accountId, beforePeriodStart);

        // Calculate totals from ledger entries
        BigDecimal totalCredits = BigDecimal.ZERO;
        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalInterest = BigDecimal.ZERO;
        BigDecimal totalFees = BigDecimal.ZERO;

        for (LedgerEntry entry : ledgerEntries) {
            if (entry.getEntryType() == LedgerEntry.EntryType.CREDIT) {
                totalCredits = totalCredits.add(entry.getAmount());
            } else {
                totalDebits = totalDebits.add(entry.getAmount());
            }

            if ("INTEREST".equalsIgnoreCase(entry.getReferenceType())) {
                totalInterest = totalInterest.add(entry.getAmount());
            } else if ("FEE".equalsIgnoreCase(entry.getReferenceType())) {
                totalFees = totalFees.add(entry.getAmount());
            }
        }

        // Calculate closing balance
        BigDecimal closingBalance = openingBalance.add(totalCredits).subtract(totalDebits);

        // Create line items with running balance
        List<StatementLineItemDto> lineItems = createLineItems(ledgerEntries, openingBalance);

        // Create and populate DTO
        StatementDto statement = new StatementDto();
        statement.setAccountId(accountId);
        statement.setAccountNumber(account.getAccountNumber());
        statement.setCustomerName(account.getCustomer().getName());
        statement.setAccountType(account.getAccountType().toString());
        statement.setAccountStatus(account.getAccountStatus().toString());
        statement.setCurrency(account.getCurrency());

        statement.setMonth(yearMonth.getMonthValue());
        statement.setYear(yearMonth.getYear());

        statement.setPeriodStart(periodStart);
        statement.setPeriodEnd(periodEnd);

        statement.setOpeningBalance(openingBalance.setScale(2, RoundingMode.HALF_UP));
        statement.setClosingBalance(closingBalance.setScale(2, RoundingMode.HALF_UP));

        statement.setTotalCredits(totalCredits.setScale(2, RoundingMode.HALF_UP));
        statement.setTotalDebits(totalDebits.setScale(2, RoundingMode.HALF_UP));
        statement.setTotalInterest(totalInterest.setScale(2, RoundingMode.HALF_UP));
        statement.setTotalFees(totalFees.setScale(2, RoundingMode.HALF_UP));

        statement.setTransactionCount((long) ledgerEntries.size());
        statement.setGeneratedAt(LocalDateTime.now());

        statement.setLineItems(lineItems);

        log.info("Statement generated successfully for account {}", accountId);
        return statement;
    }

    @Override
    @Transactional(readOnly = true)
    public StatementDto getStatement(Long accountId, YearMonth yearMonth) {
        // In this implementation, we generate on-demand
        return generateStatement(accountId, yearMonth);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StatementDto> getAccountStatements(Long accountId, Pageable pageable) {
        // Verify account exists
        accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        // For now, generate statements for last 12 months
        List<StatementDto> statements = new ArrayList<>();
        YearMonth currentMonth = YearMonth.now();

        for (int i = 0; i < 12; i++) {
            try {
                StatementDto stmt = generateStatement(accountId, currentMonth);
                statements.add(stmt);
                currentMonth = currentMonth.minusMonths(1);
            } catch (Exception e) {
                log.warn("Could not generate statement for {}", currentMonth);
            }
        }

        return new PageImpl<>(statements, pageable, statements.size());
    }

    @Override
    @Transactional(readOnly = true)
    public String exportToCsv(Long accountId, YearMonth yearMonth) {
        log.info("Exporting statement to CSV for account {} for period {}", accountId, yearMonth);

        StatementDto statement = generateStatement(accountId, yearMonth);

        try (StringWriter stringWriter = new StringWriter();
             CSVWriter csvWriter = new CSVWriter(stringWriter)) {

            // Write header
            String[] headers = {
                    "Account Statement",
                    "Account Number: " + statement.getAccountNumber(),
                    "Customer Name: " + statement.getCustomerName(),
                    "Period: " + yearMonth,
                    "",
                    "Summary",
                    "Opening Balance," + formatAmount(statement.getOpeningBalance()),
                    "Closing Balance," + formatAmount(statement.getClosingBalance()),
                    "Total Credits," + formatAmount(statement.getTotalCredits()),
                    "Total Debits," + formatAmount(statement.getTotalDebits()),
                    "Total Interest," + formatAmount(statement.getTotalInterest()),
                    "Total Fees," + formatAmount(statement.getTotalFees()),
                    "Total Transactions," + statement.getTransactionCount(),
                    "",
                    "Transaction Details",
                    "Date,Type,Amount,Description,Reference Type,Running Balance"
            };

            for (String header : headers) {
                csvWriter.writeNext(new String[]{header});
            }

            // Write line items
            for (StatementLineItemDto item : statement.getLineItems()) {
                String[] row = {
                        item.getTransactionDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                        item.getEntryType(),
                        formatAmount(item.getAmount()),
                        item.getDescription(),
                        item.getReferenceType(),
                        formatAmount(item.getRunningBalance())
                };
                csvWriter.writeNext(row);
            }

            csvWriter.flush();
            String csv = stringWriter.toString();
            log.info("CSV export completed for account {}", accountId);
            return csv;

        } catch (IOException e) {
            log.error("Error exporting to CSV", e);
            throw new RuntimeException("Error exporting statement to CSV", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportToXlsx(Long accountId, YearMonth yearMonth) {
        log.info("Exporting statement to XLSX for account {} for period {}", accountId, yearMonth);

        StatementDto statement = generateStatement(accountId, yearMonth);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Statement");
            CellStyle headerStyle = createHeaderCellStyle(workbook);
            CellStyle amountStyle = createAmountCellStyle(workbook);

            int rowNum = 0;

            // Title and basic info
            Row row = sheet.createRow(rowNum++);
            Cell cell = row.createCell(0);
            cell.setCellValue("Account Statement");
            cell.setCellStyle(headerStyle);

            row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue("Account Number:");
            row.createCell(1).setCellValue(statement.getAccountNumber());

            row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue("Customer Name:");
            row.createCell(1).setCellValue(statement.getCustomerName());

            row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue("Period:");
            row.createCell(1).setCellValue(yearMonth.toString());

            row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue("Currency:");
            row.createCell(1).setCellValue(statement.getCurrency());

            // Summary section
            rowNum++;
            row = sheet.createRow(rowNum++);
            cell = row.createCell(0);
            cell.setCellValue("Summary");
            cell.setCellStyle(headerStyle);

            rowNum = addSummaryRow(sheet, rowNum, "Opening Balance", statement.getOpeningBalance(), amountStyle);
            rowNum = addSummaryRow(sheet, rowNum, "Total Credits", statement.getTotalCredits(), amountStyle);
            rowNum = addSummaryRow(sheet, rowNum, "Total Debits", statement.getTotalDebits(), amountStyle);
            rowNum = addSummaryRow(sheet, rowNum, "Total Interest", statement.getTotalInterest(), amountStyle);
            rowNum = addSummaryRow(sheet, rowNum, "Total Fees", statement.getTotalFees(), amountStyle);
            rowNum = addSummaryRow(sheet, rowNum, "Closing Balance", statement.getClosingBalance(), amountStyle);

            row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue("Total Transactions:");
            row.createCell(1).setCellValue(statement.getTransactionCount());

            // Transaction details header
            rowNum++;
            row = sheet.createRow(rowNum++);
            cell = row.createCell(0);
            cell.setCellValue("Transaction Details");
            cell.setCellStyle(headerStyle);

            // Column headers
            row = sheet.createRow(rowNum++);
            String[] columns = {"Date", "Type", "Amount", "Description", "Reference Type", "Running Balance"};
            for (int i = 0; i < columns.length; i++) {
                Cell headerCell = row.createCell(i);
                headerCell.setCellValue(columns[i]);
                headerCell.setCellStyle(headerStyle);
            }

            // Data rows
            for (StatementLineItemDto item : statement.getLineItems()) {
                row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(item.getTransactionDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                row.createCell(1).setCellValue(item.getEntryType());
                Cell amountCell = row.createCell(2);
                amountCell.setCellValue(item.getAmount().doubleValue());
                amountCell.setCellStyle(amountStyle);
                row.createCell(3).setCellValue(item.getDescription());
                row.createCell(4).setCellValue(item.getReferenceType());
                Cell balanceCell = row.createCell(5);
                balanceCell.setCellValue(item.getRunningBalance().doubleValue());
                balanceCell.setCellStyle(amountStyle);
            }

            // Auto-size columns
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Convert to bytes
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);
            log.info("XLSX export completed for account {}", accountId);
            return bos.toByteArray();

        } catch (IOException e) {
            log.error("Error exporting to XLSX", e);
            throw new RuntimeException("Error exporting statement to XLSX", e);
        }
    }

    // Helper methods
    private BigDecimal calculateBalanceBeforeDate(Long accountId, LocalDateTime beforeDate) {
        // Get all ledger entries before the date and calculate balance
        List<LedgerEntry> entriesBeforeDate = ledgerRepository.findLedgerByAccountAndDateRange(
                accountId, LocalDateTime.of(2000, 1, 1, 0, 0, 0), beforeDate);

        BigDecimal balance = BigDecimal.ZERO;
        for (LedgerEntry entry : entriesBeforeDate) {
            if (entry.getEntryType() == LedgerEntry.EntryType.CREDIT) {
                balance = balance.add(entry.getAmount());
            } else {
                balance = balance.subtract(entry.getAmount());
            }
        }
        return balance;
    }

    private List<StatementLineItemDto> createLineItems(List<LedgerEntry> ledgerEntries, BigDecimal openingBalance) {
        List<StatementLineItemDto> lineItems = new ArrayList<>();
        BigDecimal runningBalance = openingBalance;

        for (LedgerEntry entry : ledgerEntries) {
            StatementLineItemDto lineItem = new StatementLineItemDto();
            lineItem.setTransactionDate(entry.getCreatedAt());
            lineItem.setEntryType(entry.getEntryType().toString());
            lineItem.setAmount(entry.getAmount());
            lineItem.setDescription(entry.getDescription());
            lineItem.setReferenceType(entry.getReferenceType());

            // Calculate running balance
            if (entry.getEntryType() == LedgerEntry.EntryType.CREDIT) {
                runningBalance = runningBalance.add(entry.getAmount());
            } else {
                runningBalance = runningBalance.subtract(entry.getAmount());
            }
            lineItem.setRunningBalance(runningBalance.setScale(2, RoundingMode.HALF_UP));

            lineItems.add(lineItem);
        }

        return lineItems;
    }

    private CellStyle createHeaderCellStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setFillForegroundColor(IndexedColors.BLUE.getIndex());
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createAmountCellStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("#,##0.00"));
        return style;
    }

    private int addSummaryRow(Sheet sheet, int rowNum, String label, BigDecimal amount, CellStyle amountStyle) {
        Row row = sheet.createRow(rowNum);
        row.createCell(0).setCellValue(label + ":");
        Cell cell = row.createCell(1);
        cell.setCellValue(amount.doubleValue());
        cell.setCellStyle(amountStyle);
        return rowNum + 1;
    }

    private String formatAmount(BigDecimal amount) {
        return String.format("%.2f", amount);
    }
}

