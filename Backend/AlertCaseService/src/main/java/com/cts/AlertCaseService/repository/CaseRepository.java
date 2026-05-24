package com.cts.AlertCaseService.repository;

import com.cts.AlertCaseService.entity.CaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface CaseRepository extends JpaRepository<CaseEntity, String> {
    // 2. Find cases by their status (e.g., OPEN, CLOSED)
    List<CaseEntity> findByCaseStatus(String caseStatus);

    // 3. Find all cases belonging to a specific customer
    List<CaseEntity> findByCustomerCustomerId(Long customerId);
}