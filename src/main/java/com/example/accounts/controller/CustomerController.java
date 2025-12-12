package com.example.accounts.controller;

import com.example.accounts.dto.CustomerDto;
import com.example.accounts.dto.ApiResponse;
import com.example.accounts.service.CustomerService;
import com.example.accounts.util.GeneratorUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@Tag(name = "Customer Management", description = "APIs for managing customers")
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    @Operation(summary = "Create a new customer")
    public ResponseEntity<ApiResponse> createCustomer(@Valid @RequestBody CustomerDto customerDto) throws Exception {
        String correlationId = GeneratorUtil.generateCorrelationId();
        CustomerDto created = customerService.createCustomer(customerDto);
        ApiResponse response = new ApiResponse(
            true,
            "Customer created successfully",
            correlationId,
            created,
            null
        );
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{customerId}")
    @Operation(summary = "Get customer by ID")
    public ResponseEntity<ApiResponse> getCustomer(@PathVariable Long customerId) {
        String correlationId = GeneratorUtil.generateCorrelationId();
        CustomerDto customer = customerService.getCustomerById(customerId);
        ApiResponse response = new ApiResponse(
            true,
            "Customer retrieved successfully",
            correlationId,
            customer,
            null
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get customer by user ID")
    public ResponseEntity<ApiResponse> getCustomerByUserId(@PathVariable Long userId) {
        String correlationId = GeneratorUtil.generateCorrelationId();
        CustomerDto customer = customerService.getCustomerByUserId(userId);
        ApiResponse response = new ApiResponse(
            true,
            "Customer retrieved successfully",
            correlationId,
            customer,
            null
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Get all customers with pagination")
    public ResponseEntity<ApiResponse> getAllCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String correlationId = GeneratorUtil.generateCorrelationId();
        Pageable pageable = PageRequest.of(page, size);
        Page<CustomerDto> customers = customerService.getAllCustomers(pageable);
        ApiResponse response = new ApiResponse(
            true,
            "Customers retrieved successfully",
            correlationId,
            customers,
            null
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search/{name}")
    @Operation(summary = "Search customers by name")
    public ResponseEntity<ApiResponse> searchCustomers(
            @PathVariable String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String correlationId = GeneratorUtil.generateCorrelationId();
        Pageable pageable = PageRequest.of(page, size);
        Page<CustomerDto> customers = customerService.searchCustomers(name, pageable);
        ApiResponse response = new ApiResponse(
            true,
            "Customers found",
            correlationId,
            customers,
            null
        );
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{customerId}")
    @Operation(summary = "Update customer")
    public ResponseEntity<ApiResponse> updateCustomer(
            @PathVariable Long customerId,
            @Valid @RequestBody CustomerDto customerDto) throws Exception {
        String correlationId = GeneratorUtil.generateCorrelationId();
        CustomerDto updated = customerService.updateCustomer(customerId, customerDto);
        ApiResponse response = new ApiResponse(
            true,
            "Customer updated successfully",
            correlationId,
            updated,
            null
        );
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{customerId}")
    @Operation(summary = "Delete customer")
    public ResponseEntity<ApiResponse> deleteCustomer(@PathVariable Long customerId) {
        String correlationId = GeneratorUtil.generateCorrelationId();
        customerService.deleteCustomer(customerId);
        ApiResponse response = new ApiResponse(
            true,
            "Customer deleted successfully",
            correlationId,
            null,
            null
        );
        return ResponseEntity.ok(response);
    }
}

