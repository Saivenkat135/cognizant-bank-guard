package com.cts.AlertCaseService.repository;

import com.cts.AlertCaseService.entity.CaseCustomer;
import com.cts.AlertCaseService.entity.CaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface CaseCustomerRepository extends JpaRepository<CaseCustomer, Long> {

}