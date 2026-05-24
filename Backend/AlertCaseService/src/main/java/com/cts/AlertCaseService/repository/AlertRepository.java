package com.cts.AlertCaseService.repository;

import com.cts.AlertCaseService.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertRepository extends JpaRepository<Alert, String> {
    List<Alert> findBySeverity(String severity);

    List<Alert> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
}




