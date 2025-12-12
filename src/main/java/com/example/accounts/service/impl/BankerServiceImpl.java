package com.example.accounts.service.impl;

import com.example.accounts.dto.BankerDto;
import com.example.accounts.dto.AccountsDto;
import com.example.accounts.entity.Account;
import com.example.accounts.entity.Banker;
import com.example.accounts.entity.Customer;
import com.example.accounts.exception.InvalidBankerException;
import com.example.accounts.exception.ResourceNotFoundException;
import com.example.accounts.reository.AccountRepository;
import com.example.accounts.reository.BankerRepository;
import com.example.accounts.reository.CustomerRepository;
import com.example.accounts.service.AuditService;
import com.example.accounts.service.AccountService;
import com.example.accounts.service.BankerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BankerServiceImpl implements BankerService {

    private final BankerRepository bankerRepository;
    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final AuditService auditService;
    private final AccountService accountService;

    @Override
    @Transactional
    public BankerDto createBanker(BankerDto bankerDto) {
        // Note: userId is extracted from JWT in controller and set in bankerDto

        // Validate unique constraints
        if (bankerRepository.existsByUserId(bankerDto.getUserId())) {
            throw new InvalidBankerException("A banker already exists for this user ID");
        }

        // Create banker entity
        Banker banker = new Banker();
        banker.setUserId(bankerDto.getUserId());  // From JWT token
        banker.setBranchCode(bankerDto.getBranchCode());
        banker.setPortfolio(bankerDto.getPortfolio());
        banker.setIsActive(true);

        Banker saved = bankerRepository.save(banker);

        log.info("Banker created. ID: {}, UserId (from JWT): {}", saved.getBankerId(), saved.getUserId());

        return convertToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public BankerDto getBankerById(Long bankerId) {
        Banker banker = bankerRepository.findById(bankerId)
            .orElseThrow(() -> new ResourceNotFoundException("Banker not found with ID: " + bankerId));
        return convertToDto(banker);
    }

    @Override
    @Transactional(readOnly = true)
    public BankerDto getBankerByUserId(Long userId) {
        Banker banker = bankerRepository.findByUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Banker not found for user ID: " + userId));
        return convertToDto(banker);
    }


    @Override
    @Transactional
    public BankerDto updateBanker(Long bankerId, BankerDto bankerDto) {
        Banker banker = bankerRepository.findById(bankerId)
            .orElseThrow(() -> new ResourceNotFoundException("Banker not found"));

        // Update fields
        if (bankerDto.getPortfolio() != null) {
            banker.setPortfolio(bankerDto.getPortfolio());
        }
        if (bankerDto.getBranchCode() != null) {
            banker.setBranchCode(bankerDto.getBranchCode());
        }

        Banker updated = bankerRepository.save(banker);

        log.info("Banker updated. ID: {}", bankerId);

        return convertToDto(updated);
    }

    @Override
    @Transactional
    public void deactivateBanker(Long bankerId) {
        Banker banker = bankerRepository.findById(bankerId)
            .orElseThrow(() -> new ResourceNotFoundException("Banker not found"));

        banker.setIsActive(false);
        bankerRepository.save(banker);

        log.info("Banker deactivated. ID: {}", bankerId);
    }

    @Override
    @Transactional
    public void assignCustomerToBanker(Long bankerId, Long customerId, String correlationId) {
        Banker banker = bankerRepository.findById(bankerId)
            .orElseThrow(() -> new ResourceNotFoundException("Banker not found"));

        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        customer.setAssignedBanker(banker);
        customerRepository.save(customer);

        log.info("Customer {} assigned to Banker {}. CorrelationId: {}", customerId, bankerId, correlationId);
        //auditService.logAction(bankerId, "CREATE", "CUSTOMER", customerId, correlationId, "SUCCESS");
    }

    @Override
    @Transactional
    public void unassignCustomerFromBanker(Long bankerId, Long customerId, String correlationId) {
        // ✅ Step 1: Verify banker exists
        Banker banker = bankerRepository.findById(bankerId)
            .orElseThrow(() -> new ResourceNotFoundException("Banker not found"));

        // ✅ Step 2: Verify customer exists
        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        // ✅ Step 3: Verify customer is assigned to this specific banker
        if (customer.getAssignedBanker() == null || !customer.getAssignedBanker().getBankerId().equals(bankerId)) {
            throw new InvalidBankerException(
                "Customer is not assigned to this banker. Cannot unassign from banker: " + bankerId
            );
        }

        // ✅ Step 4: Unassign customer from banker
        customer.setAssignedBanker(null);
        customerRepository.save(customer);

        log.info("Customer {} unassigned from Banker {}. CorrelationId: {}",
            customerId, bankerId, correlationId);
        //auditService.logAction(bankerId, "DELETE", "CUSTOMER", customerId, correlationId, "SUCCESS");
    }

    @Override
    @Transactional
    public void assignAccountToBanker(Long bankerId, Long accountId, String correlationId) {
        Banker banker = bankerRepository.findById(bankerId)
            .orElseThrow(() -> new ResourceNotFoundException("Banker not found"));

        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new ResourceNotFoundException("Account not found"));


        account.setAssignedBanker(banker);
        accountRepository.save(account);

        log.info("Account {} assigned to Banker {}. CorrelationId: {}", accountId, bankerId, correlationId);
       // auditService.logAction(bankerId, "CREATE", "ACCOUNT", accountId, correlationId, "SUCCESS");
    }

    @Override
    @Transactional
    public void unassignAccountFromBanker(Long bankerId, Long accountId, String correlationId) {
        // ✅ Step 1: Verify banker exists
        Banker banker = bankerRepository.findById(bankerId)
            .orElseThrow(() -> new ResourceNotFoundException("Banker not found"));

        // ✅ Step 2: Verify account exists
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        // ✅ Step 3: Verify account is assigned to this specific banker
        if (account.getAssignedBanker() == null || !account.getAssignedBanker().getBankerId().equals(bankerId)) {
            throw new InvalidBankerException(
                "Account is not assigned to this banker. Cannot unassign from banker: " + bankerId
            );
        }

        // ✅ Step 4: Unassign account from banker
        account.setAssignedBanker(null);
        accountRepository.save(account);

        log.info("Account {} unassigned from Banker {}. CorrelationId: {}",
            accountId, bankerId, correlationId);
      //  auditService.logAction(bankerId, "DELETE", "ACCOUNT", accountId, correlationId, "SUCCESS");
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Long> getAssignedCustomers(Long bankerId, Pageable pageable) {
        bankerRepository.findById(bankerId)
            .orElseThrow(() -> new ResourceNotFoundException("Banker not found"));

        return customerRepository.findByAssignedBankerBankerId(bankerId, pageable)
            .map(Customer::getCustomerId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Long> getAssignedAccounts(Long bankerId, Pageable pageable) {
        bankerRepository.findById(bankerId)
            .orElseThrow(() -> new ResourceNotFoundException("Banker not found"));

        return accountRepository.findByAssignedBankerBankerId(bankerId, pageable)
            .map(Account::getAccountId);
    }



    @Transactional
    public AccountsDto createAccountForCustomer(Long customerId, AccountsDto accountsDto, String correlationId) {
        // Note: bankerId is extracted by controller via validateCustomerAccess() which ensures banker is assigned
        // and validates the customerId access. The controller then calls this method.

        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        log.info("Creating account for Customer {} with type: {}. CorrelationId: {}",
            customerId, accountsDto.getAccountType(), correlationId);

        // Call AccountService to create the account
        AccountsDto createdAccount = accountService.createAccount(accountsDto, customerId);

        log.info("Account created for Customer {}. AccountId: {}, CorrelationId: {}",
            customerId, createdAccount.getAccountId(), correlationId);

        return createdAccount;
    }

    /**
     * Convert Banker entity to DTO
     */
    private BankerDto convertToDto(Banker banker) {
        return BankerDto.builder()
            .bankerId(banker.getBankerId())
            .userId(banker.getUserId())
            .branchCode(banker.getBranchCode())
            .portfolio(banker.getPortfolio())
            .isActive(banker.getIsActive())
            .assignedAccountIds(banker.getAssignedAccounts() != null ?
                banker.getAssignedAccounts().stream()
                    .map(Account::getAccountId)
                    .collect(Collectors.toList()) : null)
            .assignedCustomerIds(banker.getAssignedCustomers() != null ?
                banker.getAssignedCustomers().stream()
                    .map(Customer::getCustomerId)
                    .collect(Collectors.toList()) : null)
            .createdAt(banker.getCreatedAt() != null ? banker.getCreatedAt().toString() : null)
            .build();
    }
}

