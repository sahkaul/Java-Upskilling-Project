package com.example.accounts.service;

import com.example.accounts.dto.AccountsDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface AccountService {
    AccountsDto createAccount(AccountsDto accountsDto, Long customerId);

    AccountsDto getAccountById(Long accountId);

    AccountsDto getAccountByNumber(String accountNumber);

    Page<AccountsDto> getCustomerAccounts(Long customerId, Pageable pageable);

    Page<AccountsDto> getAllAccounts(Pageable pageable);

    AccountsDto updateAccount(Long accountId, AccountsDto accountsDto);

    void freezeAccount(Long accountId, String reason);

    void unfreezeAccount(Long accountId);

    void closeAccount(Long accountId, String reason);

    BigDecimal getAccountBalance(Long accountId);

    List<AccountsDto> getActiveAccountsByCustomer(Long customerId);
}

