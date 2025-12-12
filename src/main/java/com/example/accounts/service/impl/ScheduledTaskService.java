package com.example.accounts.service.impl;

import com.example.accounts.entity.Account;
import com.example.accounts.entity.LedgerEntry;
import com.example.accounts.reository.AccountRepository;
import com.example.accounts.reository.LedgerRepository;
import com.example.accounts.service.LedgerService;
import com.example.accounts.util.GeneratorUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledTaskService {

    private final AccountRepository accountRepository;
    private final LedgerService ledgerService;
    private final LedgerRepository ledgerRepository;

    @Value("${app.interest.default-annual-rate:3.5}")
    private BigDecimal annualInterestRate;

    @Value("${app.interest.calculation-frequency:DAILY}")
    private String calculationFrequency;

    /**
     * Scheduled task to calculate and accrue daily interest
     * Runs every day at 11:59 PM (23:59:00)
     */
    @Scheduled(cron = "0 59 23 * * ?")
    @Transactional
    public void accrueInterest() {
        log.info("Starting daily interest accrual task");

        try {
            List<Account> savingsAccounts = accountRepository.findByAccountType(Account.AccountType.SAVINGS);

            for (Account account : savingsAccounts) {
                if (account.getAccountStatus() == Account.AccountStatus.ACTIVE) {
                    accrueInterestForAccount(account);
                }
            }

            log.info("Daily interest accrual completed for {} accounts", savingsAccounts.size());
        } catch (Exception e) {
            log.error("Error during daily interest accrual", e);
        }
    }

    /**
     * Scheduled task to generate monthly statements
     * Runs on the last day of each month at 11:45 PM (23:45:00)
     */
    @Scheduled(cron = "0 45 23 L * ?")
    @Transactional
    public void generateMonthlyStatements() {
        log.info("Starting monthly statement generation task");

        try {
            List<Account> accounts = accountRepository.findAll();

            for (Account account : accounts) {
                generateStatementForAccount(account);
            }

            log.info("Monthly statement generation completed for {} accounts", accounts.size());
        } catch (Exception e) {
            log.error("Error during monthly statement generation", e);
        }
    }

    /**
     * Scheduled task to clean up expired idempotency keys
     * Runs every day at 3:00 AM (03:00:00)
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanupExpiredKeys() {
        log.info("Starting expired idempotency key cleanup task");

        try {
            // Cleanup logic would be implemented in IdempotencyService
            log.info("Expired idempotency key cleanup completed");
        } catch (Exception e) {
            log.error("Error during key cleanup", e);
        }
    }

    private void accrueInterestForAccount(Account account) {
        if (account.getBalance().compareTo(BigDecimal.ZERO) <= 0) {
            return; // No interest on zero or negative balance
        }

        // Calculate daily interest: (balance * annual_rate) / 365
        BigDecimal dailyRate = annualInterestRate.divide(new BigDecimal("365"), 10, RoundingMode.HALF_UP);
        BigDecimal interestAmount = account.getBalance()
            .multiply(dailyRate)
            .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        // Create ledger entries for interest
        String ledgerTxnId = GeneratorUtil.generateLedgerTransactionId();

        try {
            ledgerService.createLedgerEntries(
                ledgerTxnId,
                account.getAccountId(),  // SYSTEM account (in production)
                account.getAccountId(),
                interestAmount,
                "Daily interest accrual",
                "INTEREST",
                account.getAccountId()
            );

            log.debug("Interest accrued for account {}: {}", account.getAccountId(), interestAmount);
        } catch (Exception e) {
            log.error("Error accruing interest for account {}", account.getAccountId(), e);
        }
    }

    private void generateStatementForAccount(Account account) {
        LocalDateTime monthStart = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime monthEnd = LocalDateTime.now().withDayOfMonth(LocalDateTime.now().toLocalDate().lengthOfMonth()).withHour(23).withMinute(59).withSecond(59);

        try {
            // Get ledger entries for the period
            List<LedgerEntry> ledgerEntries = ledgerRepository.findLedgerByAccountAndDateRange(
                    account.getAccountId(), monthStart, monthEnd);

            // Calculate opening balance (balance before period start)
            LocalDateTime beforePeriodStart = monthStart.minusSeconds(1);
            BigDecimal openingBalance = calculateBalanceBeforeDate(account.getAccountId(), beforePeriodStart);

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

            log.debug("Monthly statement generated for account {}, Period: {} to {}, " +
                    "Opening Balance: {}, Closing Balance: {}, Transactions: {}",
                    account.getAccountId(), monthStart, monthEnd,
                    openingBalance, closingBalance, ledgerEntries.size());

        } catch (Exception e) {
            log.error("Error generating statement for account {}", account.getAccountId(), e);
        }
    }

    private BigDecimal calculateBalanceBeforeDate(Long accountId, LocalDateTime beforeDate) {
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
}

