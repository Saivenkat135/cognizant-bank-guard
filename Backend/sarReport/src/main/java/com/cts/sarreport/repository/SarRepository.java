package com.cts.sarreport.repository;

import com.cts.sarreport.entity.SarReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SarRepository extends JpaRepository<SarReport,Integer> {
    Optional<SarReport> findByCustomerName(String customerName);
    Optional<SarReport> findByCustomerAccountNo(String customerAccountNo);
    List<SarReport> findByStatus(String status);
    List<SarReport> findByCity(String city);
    List<SarReport> findByState(String state);
}
