package com.example.accounts.service.impl;

import com.example.accounts.dto.AccountsDto;
import com.example.accounts.entity.Account;
import com.example.accounts.exception.ResourceNotFoundException;
import com.example.accounts.reository.AccountRepository;
import com.example.accounts.reository.CustomerRepository;
import com.example.accounts.reository.LedgerRepository;
import com.example.accounts.service.AccountService;
import com.example.accounts.util.GeneratorUtil;
import com.example.accounts.util.MaskingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final LedgerRepository ledgerRepository;

    @Override
    @Transactional
    public AccountsDto createAccount(AccountsDto accountsDto, Long customerId) {
        var customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        Account account = new Account();
        account.setCustomer(customer);
        account.setAccountNumber(GeneratorUtil.generateAccountNumber());
        account.setAccountType(Account.AccountType.valueOf(accountsDto.getAccountType()));
        account.setAccountStatus(Account.AccountStatus.ACTIVE);
        account.setBalance(BigDecimal.ZERO);
        account.setCurrency(accountsDto.getCurrency() != null ? accountsDto.getCurrency() : "USD");
        account.setBranchAddress(accountsDto.getBranchAddress());
        account.setEncryptionVersion(1);
        account.setLastEncryptedOn(System.currentTimeMillis());

        Account saved = accountRepository.save(account);
        log.info("Account created with number: {}", saved.getAccountNumber());
        return convertToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountsDto getAccountById(Long accountId) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new ResourceNotFoundException("Account not found with ID: " + accountId));
        return convertToDto(account);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountsDto getAccountByNumber(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
            .orElseThrow(() -> new ResourceNotFoundException("Account not found with number: " + accountNumber));
        return convertToDto(account);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AccountsDto> getCustomerAccounts(Long customerId, Pageable pageable) {
        return accountRepository.findByCustomerCustomerId(customerId, pageable)
            .map(this::convertToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AccountsDto> getAllAccounts(Pageable pageable) {
        return accountRepository.findAll(pageable)
            .map(this::convertToDto);
    }


    //for updating branch address and balance only
    @Override
    @Transactional
    public AccountsDto updateAccount(Long accountId, AccountsDto accountsDto) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        if (accountsDto.getBranchAddress() != null) {
            account.setBranchAddress(accountsDto.getBranchAddress());
        }

        if (accountsDto.getBalance() != null) {
            account.setBalance(accountsDto.getBalance());
        }

        Account updated = accountRepository.save(account);
        return convertToDto(updated);
    }

    @Override
    @Transactional
    public void freezeAccount(Long accountId, String reason) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        account.setAccountStatus(Account.AccountStatus.FROZEN);
        account.setFrozenReason(reason);
        account.setFrozenOn(LocalDateTime.now());
        accountRepository.save(account);
        log.info("Account frozen: {}", accountId);
    }

    @Override
    @Transactional
    public void unfreezeAccount(Long accountId) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        account.setAccountStatus(Account.AccountStatus.ACTIVE);
        account.setFrozenReason("Manually unfrozen");
        account.setFrozenOn(LocalDateTime.now());
        accountRepository.save(account);
        log.info("Account unfrozen: {}", accountId);
    }

    @Override
    @Transactional
    public void closeAccount(Long accountId, String reason) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        account.setAccountStatus(Account.AccountStatus.CLOSED);
        account.setClosedReason(reason);
        account.setClosedOn(LocalDateTime.now());
        accountRepository.save(account);
        log.info("Account closed: {}", accountId);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getAccountBalance(Long accountId) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        // Calculate from ledger for correctness
        BigDecimal balanceFromLedger = ledgerRepository.calculateAccountBalance(accountId);
        return balanceFromLedger != null ? balanceFromLedger : BigDecimal.ZERO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountsDto> getActiveAccountsByCustomer(Long customerId) {
        return accountRepository.findActiveAccountsByCustomer(customerId).stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
    }

    private AccountsDto convertToDto(Account account) {
        AccountsDto dto = new AccountsDto();
        dto.setAccountId(account.getAccountId());
        dto.setCustomerId(account.getCustomer().getCustomerId());
        dto.setAccountNumber(account.getAccountNumber());
        dto.setMaskedAccountNumber(MaskingUtil.maskAccountNumber(account.getAccountNumber()));
        dto.setAccountType(account.getAccountType().toString());
        dto.setAccountStatus(account.getAccountStatus().toString());
        dto.setBalance(account.getBalance());
        dto.setCurrency(account.getCurrency());
        dto.setBranchAddress(account.getBranchAddress());
        dto.setCreatedAt(account.getCreatedAt());
        dto.setUpdatedAt(account.getUpdatedAt());
        dto.setFrozenReason(account.getFrozenReason());
        dto.setClosedReason(account.getClosedReason());
        return dto;
    }
}

