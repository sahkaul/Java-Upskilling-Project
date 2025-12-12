package com.example.accounts.service;

import com.example.accounts.dto.LedgerEntryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface LedgerService {
    void createLedgerEntries(String ledgerTxnId, Long sourceAccountId, Long destinationAccountId, BigDecimal amount, String description, String referenceType, Long referenceId);

    Page<LedgerEntryDto> getAccountLedger(Long accountId, Pageable pageable);

    BigDecimal calculateAccountBalance(Long accountId);

    List<LedgerEntryDto> getLedgerByTransactionId(String ledgerTxnId);
}

