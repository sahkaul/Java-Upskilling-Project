package com.example.accounts.service.impl;

import com.example.accounts.crypto.EncryptionService;
import com.example.accounts.dto.CustomerDto;
import com.example.accounts.entity.Customer;
import com.example.accounts.exception.ResourceNotFoundException;
import com.example.accounts.reository.CustomerRepository;
import com.example.accounts.service.CustomerService;
import com.example.accounts.util.MaskingUtil;
import com.example.accounts.util.SecurityContextUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final EncryptionService encryptionService;

    @Override
    @Transactional
    public CustomerDto createCustomer(CustomerDto customerDto) throws Exception {
        if (customerRepository.existsByEmail(customerDto.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        // Extract userId from JWT token
        Long userId = SecurityContextUtil.getCurrentUserContext().getUserId();

        Customer customer = new Customer();
        customer.setUserId(userId);
        customer.setName(customerDto.getName());
        customer.setEmail(encryptionService.encrypt(customerDto.getEmail()));
        customer.setPhoneNumber(encryptionService.encrypt(customerDto.getPhoneNumber()));
        customer.setAddress(encryptionService.encrypt(customerDto.getAddress()));
        customer.setKycId(customerDto.getKycId());
        customer.setEncryptionVersion(1);
        customer.setLastEncryptedOn(System.currentTimeMillis());

        Customer saved = customerRepository.save(customer);
        log.info("Customer created with ID: {}", saved.getCustomerId());
        return convertToDto(saved, false);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerDto getCustomerById(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + customerId));
        return convertToDto(customer, false);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerDto getCustomerByUserId(Long userId) {
        Customer customer = customerRepository.findByUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Customer not found with UserId: " + userId));
        return convertToDto(customer, false);
    }

    @Override
    @Transactional
    public CustomerDto updateCustomer(Long customerId, CustomerDto customerDto) throws Exception {
        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + customerId));

        customer.setName(customerDto.getName());
        if (customerDto.getPhoneNumber() != null) {
            customer.setPhoneNumber(encryptionService.encrypt(customerDto.getPhoneNumber()));
        }
        if (customerDto.getAddress() != null) {
            customer.setAddress(encryptionService.encrypt(customerDto.getAddress()));
        }
        customer.setEncryptionVersion(1);
        customer.setLastEncryptedOn(System.currentTimeMillis());

        Customer updated = customerRepository.save(customer);
        log.info("Customer updated with ID: {}", customerId);
        return convertToDto(updated, false);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerDto> getAllCustomers(Pageable pageable) {
        return customerRepository.findAll(pageable)
            .map(c -> convertToDto(c, false));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerDto> searchCustomers(String name, Pageable pageable) {
        return customerRepository.findByNameContainingIgnoreCase(name, pageable)
            .map(c -> convertToDto(c, false));
    }

    @Override
    @Transactional
    public void deleteCustomer(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + customerId));
        customerRepository.delete(customer);
        log.info("Customer deleted with ID: {}", customerId);
    }

    private CustomerDto convertToDto(Customer customer, boolean includeSensitive) {
        CustomerDto dto = new CustomerDto();
        dto.setCustomerId(customer.getCustomerId());
        dto.setUserId(customer.getUserId());
        dto.setName(customer.getName());
        dto.setKycId(customer.getKycId());

        try {
            String decryptedEmail = encryptionService.decrypt(customer.getEmail());
            String decryptedPhone = customer.getPhoneNumber() != null ? encryptionService.decrypt(customer.getPhoneNumber()) : null;
            String decryptedAddress = customer.getAddress() != null ? encryptionService.decrypt(customer.getAddress()) : null;

            if (includeSensitive) {
                dto.setEmail(decryptedEmail);
                dto.setPhoneNumber(decryptedPhone);
                dto.setAddress(decryptedAddress);
            } else {
                dto.setMaskedEmail(MaskingUtil.maskEmail(decryptedEmail));
                dto.setMaskedPhoneNumber(MaskingUtil.maskPhoneNumber(decryptedPhone));
                dto.setMaskedAddress(MaskingUtil.maskAddress(decryptedAddress));
            }
        } catch (Exception e) {
            log.error("Error decrypting customer data", e);
        }

        return dto;
    }
}

