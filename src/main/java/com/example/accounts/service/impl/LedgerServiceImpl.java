package com.example.accounts.service.impl;

import com.example.accounts.dto.LedgerEntryDto;
import com.example.accounts.entity.Account;
import com.example.accounts.entity.LedgerEntry;
import com.example.accounts.reository.AccountRepository;
import com.example.accounts.reository.LedgerRepository;
import com.example.accounts.service.LedgerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerServiceImpl implements LedgerService {

    private final LedgerRepository ledgerRepository;
    private final AccountRepository accountRepository;

    @Override
    @Transactional
    public void createLedgerEntries(String ledgerTxnId, Long sourceAccountId, Long destinationAccountId,
                                    BigDecimal amount, String description, String referenceType, Long referenceId) {

        Account sourceAccount = accountRepository.findById(sourceAccountId)
            .orElseThrow(() -> new RuntimeException("Source account not found"));
        Account destinationAccount = accountRepository.findById(destinationAccountId)
            .orElseThrow(() -> new RuntimeException("Destination account not found"));

        // Create DEBIT entry for source account
        LedgerEntry debitEntry = new LedgerEntry();
        debitEntry.setLedgerTxnId(ledgerTxnId);
        debitEntry.setAccount(sourceAccount);
        debitEntry.setEntryType(LedgerEntry.EntryType.DEBIT);
        debitEntry.setAmount(amount);
        debitEntry.setDescription(description);
        debitEntry.setReferenceType(referenceType);
        debitEntry.setReferenceId(referenceId);
        ledgerRepository.save(debitEntry);

        // Create CREDIT entry for destination account
        LedgerEntry creditEntry = new LedgerEntry();
        creditEntry.setLedgerTxnId(ledgerTxnId);
        creditEntry.setAccount(destinationAccount);
        creditEntry.setEntryType(LedgerEntry.EntryType.CREDIT);
        creditEntry.setAmount(amount);
        creditEntry.setDescription(description);
        creditEntry.setReferenceType(referenceType);
        creditEntry.setReferenceId(referenceId);
        ledgerRepository.save(creditEntry);

        // Update account balances
        sourceAccount.setBalance(sourceAccount.getBalance().subtract(amount));
        destinationAccount.setBalance(destinationAccount.getBalance().add(amount));
        accountRepository.save(sourceAccount);
        accountRepository.save(destinationAccount);

        log.info("Ledger entries created. TxnId: {}, Amount: {}", ledgerTxnId, amount);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LedgerEntryDto> getAccountLedger(Long accountId, Pageable pageable) {
        return ledgerRepository.findByAccountId(accountId, pageable)
            .map(this::convertToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateAccountBalance(Long accountId) {
        return ledgerRepository.calculateAccountBalance(accountId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LedgerEntryDto> getLedgerByTransactionId(String ledgerTxnId) {
        return ledgerRepository.findByLedgerTxnId(ledgerTxnId).stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
    }

    private LedgerEntryDto convertToDto(LedgerEntry ledgerEntry) {
        return new LedgerEntryDto(
            ledgerEntry.getLedgerEntryId(),
            ledgerEntry.getLedgerTxnId(),
            ledgerEntry.getAccount().getAccountId(),
            ledgerEntry.getEntryType().toString(),
            ledgerEntry.getAmount(),
            ledgerEntry.getDescription(),
            ledgerEntry.getReferenceType(),
            ledgerEntry.getReferenceId()
        );
    }
}

