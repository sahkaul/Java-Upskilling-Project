package com.example.accounts.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "LedgerEntry", description = "Schema for ledger entry")
public class LedgerEntryDto {

    private Long ledgerEntryId;

    private String ledgerTxnId;

    private Long accountId;

    private String entryType;  // DEBIT or CREDIT

    private BigDecimal amount;

    private String description;

    private String referenceType;

    private Long referenceId;
}

