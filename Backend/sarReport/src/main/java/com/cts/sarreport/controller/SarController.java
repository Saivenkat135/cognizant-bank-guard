package com.cts.sarreport.controller;

import com.cts.sarreport.dto.ReportingRequest;
import com.cts.sarreport.entity.SarReport;
import com.cts.sarreport.service.SarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/sar")
@Tag(name = "SAR Management", description = "Endpoints for creating and retrieving SAR reports")
@Slf4j
public class SarController {

    @Autowired
    private SarService sarService;


    // ==========================================
    //  ALERT CASE SERVICE INTEGRATION ENDPOINT
    // ==========================================
    /**
     * Receive ReportingRequest from AlertCaseService
     * This endpoint accepts fraud alert case data and stores it as SAR Report
     */
    @PostMapping("/ingest-report")
    @Operation(summary = "Ingest reporting request from Alert Case Service",
            description = "Receives ReportingRequest DTO from AlertCaseService and stores as SAR Report")
    public ResponseEntity<SarReport> ingestReportingRequest(@RequestBody ReportingRequest reportingRequest) {
        try {
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.info("RECEIVED REPORTING REQUEST FROM ALERT CASE SERVICE");
            log.info("Case ID: {}, Customer ID: {}, Risk Score: {}, Transaction ID: {}",
                    reportingRequest.getCaseId(),
                    reportingRequest.getCustomerId(),
                    reportingRequest.getRiskScore()
                    );
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            SarReport savedReport = sarService.processReportingRequest(reportingRequest);

            log.info("✓ Reporting request processed successfully. SAR ID: {}", savedReport.getSarId());
            System.out.println("✓ Reporting request processed and SAR Report created: " + savedReport.getSarId());

            return new ResponseEntity<>(savedReport, HttpStatus.CREATED);
        } catch (Exception e) {
            log.error("✗ Error processing reporting request: {}", e.getMessage(), e);
            System.err.println("✗ Error processing reporting request: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

//    @PostMapping("/report")
//    public ResponseEntity<SarReport> pushDetails(@RequestBody SarReport report) {
//        SarReport savedReport = sarService.putDetails(report);
//        return new ResponseEntity<>(savedReport, HttpStatus.CREATED);
//    }

    @GetMapping("/reports")
    public ResponseEntity<List<SarReport>> getAllReports() {
        try {
            List<SarReport> reports = sarService.getAllReports();
            return ResponseEntity.ok(reports);
        } catch (Exception e) {
            e.printStackTrace(); // This will print the error in your console
            throw e;
        }
    }

    @Operation(summary = "Get report by ID", description = "Returns a single SAR report based on the internal ID")
    @GetMapping("/report/id/{sarId}")
    public ResponseEntity<SarReport> generateReportById(@PathVariable int sarId) {
        return ResponseEntity.ok(sarService.generateById(sarId));
    }

    @GetMapping("/report/name/{customerName}")
    public ResponseEntity<SarReport> generateReportByName(@PathVariable String customerName) {
        return ResponseEntity.ok(sarService.generateByName(customerName));
    }

    @GetMapping("/report/account/{customerAccountNo}")
    public ResponseEntity<SarReport> generateReportByAccountNo(@PathVariable String customerAccountNo) {
        return ResponseEntity.ok(sarService.generateByAccountNo(customerAccountNo));
    }

    @GetMapping("/report/status/{status}")
    public ResponseEntity<List<SarReport>> generateReportByStatus(@PathVariable String status) {
        return ResponseEntity.ok(sarService.generateByStatus(status));
    }

//    @GetMapping("/report/transaction/{transactionId}")
//    public ResponseEntity<SarReport> generateReportByTransactionId(@PathVariable Long transactionId) {
//        return ResponseEntity.ok(sarService.generateByTransactionId(transactionId));
//    }

    @GetMapping("/report/city/{city}")
    public ResponseEntity<List<SarReport>> generateReportByCity(@PathVariable String city) {
        return ResponseEntity.ok(sarService.generateByCity(city));
    }

    @GetMapping("/report/state/{state}")
    public ResponseEntity<List<SarReport>> generateReportByState(@PathVariable String state) {
        return ResponseEntity.ok(sarService.generateByState(state));
    }

}