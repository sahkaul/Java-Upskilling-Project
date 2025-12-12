package com.example.accounts.service;

import com.example.accounts.dto.CustomerDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomerService {
    CustomerDto createCustomer(CustomerDto customerDto) throws Exception;

    CustomerDto getCustomerById(Long customerId);

    CustomerDto getCustomerByUserId(Long userId);

    CustomerDto updateCustomer(Long customerId, CustomerDto customerDto) throws Exception;

    Page<CustomerDto> getAllCustomers(Pageable pageable);

    Page<CustomerDto> searchCustomers(String name, Pageable pageable);

    void deleteCustomer(Long customerId);
}

