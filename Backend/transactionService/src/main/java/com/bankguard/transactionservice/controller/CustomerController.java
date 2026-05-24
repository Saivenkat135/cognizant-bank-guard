package com.bankguard.transactionservice.controller;

import com.bankguard.transactionservice.dto.CustomerEnrichmentDTO;
import com.bankguard.transactionservice.dto.LoginRequest;
import com.bankguard.transactionservice.entity.Customer;
import com.bankguard.transactionservice.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    @PostMapping
    public ResponseEntity<CustomerEnrichmentDTO> createCustomer(@RequestBody Customer customer) {
        CustomerEnrichmentDTO savedCustomer = customerService.saveCustomer(customer);
        return new ResponseEntity<>(savedCustomer, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<CustomerEnrichmentDTO> loginCustomer(@RequestBody LoginRequest request) {
        CustomerEnrichmentDTO dto = customerService.loginCustomer(request.getEmail(), request.getPassword());
        if (dto == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        return new ResponseEntity<>(dto, HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<List<Customer>> getAllCustomers() {
        List<Customer> customers = customerService.getAllCustomers();
        return new ResponseEntity<>(customers, HttpStatus.OK);
    }

    @GetMapping("/{customerId}")
    public ResponseEntity<CustomerEnrichmentDTO> getCustomerById(@PathVariable Long customerId) {
        Customer customer = customerService.getCustomerById(customerId);
        if (customer != null) {
            CustomerEnrichmentDTO customerEnrichmentDTO = new CustomerEnrichmentDTO();
            customerEnrichmentDTO.setName(customer.getName());
            customerEnrichmentDTO.setEmail(customer.getEmail());
            customerEnrichmentDTO.setBalance(customer.getBalance());
            customerEnrichmentDTO.setAccountType(customer.getAccountType());
            customerEnrichmentDTO.setBankName(customer.getBankName());
            customerEnrichmentDTO.setCustomerId(customer.getCustomerId());
            customerEnrichmentDTO.setAccountNo(customer.getAccountNo());
            return new ResponseEntity<>(customerEnrichmentDTO, HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @PutMapping("/{customerId}")
    public ResponseEntity<Customer> updateCustomer(@PathVariable Long customerId, @RequestBody Customer customerDetails) {
        Customer updatedCustomer = customerService.updateCustomer(customerId, customerDetails);
        if (updatedCustomer != null) {
            return new ResponseEntity<>(updatedCustomer, HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("/{customerId}")
    public ResponseEntity<String> deleteCustomer(@PathVariable Long customerId) {
        boolean deleted = customerService.deleteCustomer(customerId);
        if (deleted) {
            return new ResponseEntity<>("Customer deleted successfully", HttpStatus.OK);
        }
        return new ResponseEntity<>("Customer not found", HttpStatus.NOT_FOUND);
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<CustomerEnrichmentDTO> getCustomerByEmail(@PathVariable String email) {
        Customer customer = customerService.getCustomerByEmail(email);
        if (customer != null) {
            CustomerEnrichmentDTO customerEnrichmentDTO = new CustomerEnrichmentDTO();
            customerEnrichmentDTO.setName(customer.getName());
            customerEnrichmentDTO.setEmail(customer.getEmail());
            customerEnrichmentDTO.setBalance(customer.getBalance());
            customerEnrichmentDTO.setAccountType(customer.getAccountType());
            customerEnrichmentDTO.setBankName(customer.getBankName());
            customerEnrichmentDTO.setCustomerId(customer.getCustomerId());
            customerEnrichmentDTO.setAccountNo(customer.getAccountNo());
            return new ResponseEntity<>(customerEnrichmentDTO, HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @GetMapping("/account/{accountNo}")
    public ResponseEntity<CustomerEnrichmentDTO> getCustomerByAccountNo(@PathVariable String accountNo) {
        Customer customer = customerService.getCustomerByAccountNo(accountNo);
        if (customer != null) {
            CustomerEnrichmentDTO customerEnrichmentDTO = new CustomerEnrichmentDTO();
            customerEnrichmentDTO.setName(customer.getName());
            customerEnrichmentDTO.setEmail(customer.getEmail());
            customerEnrichmentDTO.setBalance(customer.getBalance());
            customerEnrichmentDTO.setAccountType(customer.getAccountType());
            customerEnrichmentDTO.setBankName(customer.getBankName());
            customerEnrichmentDTO.setCustomerId(customer.getCustomerId());
            customerEnrichmentDTO.setAccountNo(customer.getAccountNo());
            return new ResponseEntity<>(customerEnrichmentDTO, HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
}
