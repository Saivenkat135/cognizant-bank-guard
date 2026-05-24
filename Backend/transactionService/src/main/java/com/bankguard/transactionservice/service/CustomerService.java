package com.bankguard.transactionservice.service;

import java.util.List;
import java.util.Optional;

import com.bankguard.transactionservice.dto.CustomerEnrichmentDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bankguard.transactionservice.entity.Customer;
import com.bankguard.transactionservice.repository.CustomerRepository;

@Service
public class CustomerService {

    @Autowired
    private CustomerRepository customerRepository;

    public CustomerEnrichmentDTO saveCustomer(Customer customer) {
        // Set default riskScore if null
        if (customer.getRiskScore() == null) {
            customer.setRiskScore(0.0);
        }
        Customer cust = customerRepository.save(customer);
        CustomerEnrichmentDTO customerEnrichmentDTO = new CustomerEnrichmentDTO();
        customerEnrichmentDTO.setName(customer.getName());
        customerEnrichmentDTO.setBalance(customer.getBalance());
        customerEnrichmentDTO.setAccountType(customer.getAccountType());
        customerEnrichmentDTO.setEmail(customer.getEmail());
        customerEnrichmentDTO.setBankName(customer.getBankName());
        customerEnrichmentDTO.setCustomerId(customer.getCustomerId());
        customerEnrichmentDTO.setAccountNo(customer.getAccountNo());
        return customerEnrichmentDTO;
    }

    public Customer getCustomerById(Long customerId) {
        Optional<Customer> customer = customerRepository.findById(customerId);
        return customer.orElse(null);
    }

    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    public Customer updateCustomer(Long customerId, Customer customerDetails) {
        Optional<Customer> existingCustomer = customerRepository.findById(customerId);
        if (existingCustomer.isPresent()) {
            Customer customer = existingCustomer.get();
            customer.setBankName(customerDetails.getBankName());
            customer.setBalance(customerDetails.getBalance());
            customer.setAccountType(customerDetails.getAccountType());
            customer.setName(customerDetails.getName());
            customer.setEmail(customerDetails.getEmail());
            customer.setAccountNo(customerDetails.getAccountNo());
            return customerRepository.save(customer);
        }
        return null;
    }

    public boolean deleteCustomer(Long customerId) {
        if (customerRepository.existsById(customerId)) {
            customerRepository.deleteById(customerId);
            return true;
        }
        return false;
    }

    public Customer getCustomerByEmail(String email) {
        return customerRepository.findByEmail(email);
    }

    public Customer getCustomerByAccountNo(String accountNo) {
        return customerRepository.findByAccountNo(accountNo);
    }

    public CustomerEnrichmentDTO loginCustomer(String email, String password) {
        Customer customer = customerRepository.findByEmail(email);
        if (customer == null) return null;
        if (customer.getPassword() == null || !customer.getPassword().equals(password)) return null;

        CustomerEnrichmentDTO dto = new CustomerEnrichmentDTO();
        dto.setCustomerId(customer.getCustomerId());
        dto.setName(customer.getName());
        dto.setEmail(customer.getEmail());
        dto.setBankName(customer.getBankName());
        dto.setBalance(customer.getBalance());
        dto.setAccountType(customer.getAccountType());
        dto.setAccountNo(customer.getAccountNo());
        return dto;
    }
}