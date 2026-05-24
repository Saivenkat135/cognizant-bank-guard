package com.bankguard.transactionservice.repository;

import com.bankguard.transactionservice.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Customer findByEmail(String email);
    Customer findByAccountNo(String accountNo);
}
