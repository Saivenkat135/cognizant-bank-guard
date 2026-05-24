package com.cts.AlertCaseService.controller;

import com.cts.AlertCaseService.dto.AlertCasePayload;
import com.cts.AlertCaseService.entity.Alert;
import com.cts.AlertCaseService.entity.CaseEntity;
import com.cts.AlertCaseService.entity.CaseCustomer;
import com.cts.AlertCaseService.exception.InvalidPayloadException;
import com.cts.AlertCaseService.service.FraudInvestigationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/investigation")
@RequiredArgsConstructor
@Slf4j
public class AlertCaseController {

    private final FraudInvestigationService investigationService;

    // --- NEW FRAUD ALERT INGEST API ---
    // Receives fraud alert from enrichmentService with AlertCasePayload
    @PostMapping("/ingest-fraud-alert")
    public ResponseEntity<Void> ingestFraudAlert(@RequestBody AlertCasePayload payload) {
        // Validate incoming payload
        if (payload == null) {
            throw new InvalidPayloadException("AlertCasePayload cannot be null");
        }



        if (payload.getGeminiRiskScore() == null) {
            throw new InvalidPayloadException(
                    "Invalid payload: geminiRiskScore is required",
                    "Missing required field: geminiRiskScore"
            );
        }

        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("RECEIVED FRAUD ALERT FROM ENRICHMENT SERVICE");
        log.info("Decision: {}, Risk Score: {}, Amount: {}",
                payload.getDecisionStatus(),
                payload.getGeminiRiskScore(),
                payload.getAmount());
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        investigationService.processAlertCasePayload(payload);

        log.info("✓ Fraud alert processed successfully");
        System.out.println("✓ Fraud alert processed and case created");
        return ResponseEntity.ok().build();
    }

    // ==========================================
    //            ALERT ENDPOINTS
    // ==========================================

    @GetMapping("/alerts")
    public ResponseEntity<List<Alert>> getAllAlerts() {
        return ResponseEntity.ok(investigationService.getAllAlerts());
    }

    @GetMapping("/alerts/{alertId}")
    public ResponseEntity<Alert> getAlertById(@PathVariable String alertId) {
        return ResponseEntity.ok(investigationService.getAlertById(alertId));
    }

    @GetMapping("/alerts/severity/{severity}")
    public ResponseEntity<List<Alert>> getAlertsBySeverity(@PathVariable String severity) {
        return ResponseEntity.ok(investigationService.getAlertsBySeverity(severity));
    }

    // Customer-facing: list alerts for a single customer (used by Messages tab)
    @GetMapping("/alerts/customer/{customerId}")
    public ResponseEntity<List<Alert>> getAlertsByCustomer(@PathVariable Long customerId) {
        return ResponseEntity.ok(investigationService.getAlertsByCustomerId(customerId));
    }

    // ==========================================
    //            CASE ENDPOINTS
    // ==========================================

    @GetMapping("/cases/{caseId}")
    public ResponseEntity<CaseEntity> getCaseById(@PathVariable String caseId) {
        return ResponseEntity.ok(investigationService.getCaseById(caseId));
    }

    @GetMapping("/cases/status/{status}")
    public ResponseEntity<List<CaseEntity>> getCasesByStatus(@PathVariable String status) {
        return ResponseEntity.ok(investigationService.getCasesByStatus(status));
    }

    // This is a PUT request because it modifies existing data
    @PutMapping("/cases/{caseId}/status")
    public ResponseEntity<CaseEntity> updateCaseStatus(
            @PathVariable String caseId,
            @RequestParam String status) {
        return ResponseEntity.ok(investigationService.updateCaseStatus(caseId, status));
    }

    // ==========================================
    //          CUSTOMER ENDPOINTS
    // ==========================================

    @GetMapping("/customers/{customerId}")
    public ResponseEntity<CaseCustomer> getCustomerDetails(@PathVariable Long customerId) {
        return ResponseEntity.ok(investigationService.getCustomerWithCases(customerId));
    }

    @GetMapping("/customers/{customerId}/cases")
    public ResponseEntity<List<CaseEntity>> getCasesByCustomer(@PathVariable Long customerId) {
        return ResponseEntity.ok(investigationService.getCasesByCustomerId(customerId));
    }
}
