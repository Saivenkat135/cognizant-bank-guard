package com.cts.AlertCaseService.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cts.AlertCaseService.client.ReportingClient;
import com.cts.AlertCaseService.dto.AlertCasePayload;
import com.cts.AlertCaseService.dto.ReportingRequest;
import com.cts.AlertCaseService.entity.Alert;
import com.cts.AlertCaseService.entity.CaseCustomer;
import com.cts.AlertCaseService.entity.CaseEntity;
import com.cts.AlertCaseService.exception.AlertNotFoundException;
import com.cts.AlertCaseService.exception.AlertProcessingException;
import com.cts.AlertCaseService.exception.CaseNotFoundException;
import com.cts.AlertCaseService.exception.CustomerNotFoundException;
import com.cts.AlertCaseService.repository.AlertRepository;
import com.cts.AlertCaseService.repository.CaseCustomerRepository;
import com.cts.AlertCaseService.repository.CaseRepository;

import com.cts.AlertCaseService.service.EmailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudInvestigationService {

    private final AlertRepository alertRepo;
    private final CaseRepository caseRepo;
    private final CaseCustomerRepository customerRepo;
    private final ReportingClient reportingClient;

    private final EmailService emailService;


    /**
     * Process AlertCasePayload from enrichmentService (NEW METHOD)
     * Handles the consolidated fraud alert structure
     */
    @Transactional
    public void processAlertCasePayload(AlertCasePayload payload) {
        log.info("Processing AlertCasePayload: Decision={}, Risk Score={}, Customer ID will be generated",
                payload.getDecisionStatus(), payload.getGeminiRiskScore());
        
        try {
            // Extract data from payload
            Double riskScore = payload.getGeminiRiskScore() != null ? payload.getGeminiRiskScore() : 0.0;
            String customerName = payload.getCustomerName();
            Double amount = payload.getAmount();
            String decision = payload.getDecisionStatus();
            
            // Generate customer ID if not provided (use timestamp-based)
            Long customerId = payload.getCustomerId();
            
            // 1. Create and persist Alert
            Alert alert = new Alert();
            alert.setSeverity(riskScore > 80 ? "HIGH" : riskScore > 60 ? "MEDIUM" : "LOW");
            alert.setDecisionStatus(decision);
            alert.setRiskScore(riskScore);
            alert.setReason(payload.getReason());
            alert.setCustomerId(payload.getCustomerId());
            alert.setCreatedAt(LocalDateTime.now());
            alertRepo.save(alert);
            log.info("Alert created: {}", alert.getAlertId());

            // 2. Create/retrieve Customer
            CaseCustomer customer = customerRepo.findById(payload.getCustomerId())
                    .orElseGet(() -> {
                        CaseCustomer newCustomer = new CaseCustomer();
                        newCustomer.setCustomerId(customerId);
                        return customerRepo.save(newCustomer);
                    });
            log.info("Customer handled: {}", customerId);

            // 3. Create and persist Case
            CaseEntity fraudCase = new CaseEntity();
            fraudCase.setAlertId(alert.getAlertId());
            fraudCase.setCaseStatus("OPEN");
            fraudCase.setReason("Fraud Alert: " + payload.getReason());
            fraudCase.setRiskScore(riskScore);
            fraudCase.setDecisionStatus(payload.getDecisionStatus());
            fraudCase.setAmount(amount);
            fraudCase.setCustomerName(customerName);
            fraudCase.setCreatedAt(LocalDateTime.now());
            fraudCase.setCustomer(customer);
            caseRepo.save(fraudCase);
            log.info("Case created: {} for Alert: {}", fraudCase.getCaseId(), alert.getAlertId());

            // 4. Forward to Reporting Service with enriched customer data
            // Extract enriched transaction data to pass as customerPayload
//            Object enrichedData = null;
//            if (payload.getEnrichedTransaction() != null) {
//                enrichedData = payload.getEnrichedTransaction();
//                log.debug("Including enriched transaction data in ReportingRequest");
//            }

            String customerEmail=payload.getCustomerEmail();

            StringBuilder emailMessage = new StringBuilder();
            emailMessage.append("Dear Customer,\n\n");
            emailMessage.append("This is an automated notification to inform you that a recent transaction on your account has been ")
                    .append(payload.getDecisionStatus()) // terminated / flagged
                    .append(" by our security decision engine.\n\n");

            emailMessage.append("Reason for Action:\n");
            emailMessage.append("- ").append(payload.getReason()).append("\n\n");

            emailMessage.append("Transaction Details for Your Reference:\n");
            emailMessage.append("------------------------------------------\n");
            emailMessage.append("Timestamp:        ").append(payload.getTime()).append("\n");
            emailMessage.append("Sender Account:   ").append(payload.getCustomerAccountNo()).append("\n");
//            emailMessage.append("Receiver Account: ").append(receiverAccount).append("\n");
            emailMessage.append("Amount:           ").append(payload.getAmount()).append("\n");
            emailMessage.append("------------------------------------------\n\n");

            emailMessage.append("If you believe this action was taken in error, or if you did not authorize this activity, please contact our fraud prevention department immediately via the secure message center in your mobile app.\n\n");
            emailMessage.append("For your security, please do not share your account credentials or one-time passwords (OTP) with anyone.\n\n");
            emailMessage.append("Regards,\n");
            emailMessage.append("Global Security & Fraud Prevention Team");

            String finalMessage = emailMessage.toString();

            String subject="Important Security Notice Regarding Your Recent Transaction";

            try {
                if (customerEmail != null && !customerEmail.isBlank()) {
                    emailService.sendEmail(customerEmail, subject, finalMessage);
                    log.info("✓ Customer notification email sent to {}", customerEmail);
                } else {
                    log.warn("Skipping customer email: customerEmail is null/blank for caseId={}", fraudCase.getCaseId());
                }
            } catch (Exception emailEx) {
                log.error("❌ Failed to send customer email to {} for caseId={}: {}",
                        customerEmail, fraudCase.getCaseId(), emailEx.getMessage(), emailEx);
            }

            ReportingRequest reportingReq = new ReportingRequest(
                    fraudCase.getCaseId(),
                    fraudCase.getCaseStatus(),
                    riskScore,
                    fraudCase.getReason(),
                    payload.getDecisionStatus(),
                    amount,
                    payload.getCity(),
                    payload.getState(),
                    payload.getCustomerEmail(),
                    payload.getCustomerId(),
                    payload.getCustomerAccountNo(),
                    payload.getCustomerBalance(),
                    customerName,
                    LocalDateTime.now()
                    );
            reportingClient.sendToReporting(reportingReq);
            log.info("Forwarded to ReportingService");
            System.out.println("✓ Case created: " + fraudCase.getCaseId() + " | Alert: " + alert.getAlertId());

        } catch (Exception e) {
            log.error("Error processing AlertCasePayload: {}", e.getMessage(), e);
            throw new AlertProcessingException(
                    "Failed to process fraud alert: " + e.getMessage(),
                    "Error while creating alert and case records",
                    e
            );
        }
    }

    // API 1: Get Alert by ID
    public Alert getAlertById(String alertId) {
        return alertRepo.findById(alertId)
                .orElseThrow(() -> new AlertNotFoundException(alertId));
    }

    // API 2: Get All Alerts
    public List<Alert> getAllAlerts() {
        return alertRepo.findAll();
    }

    // API 3: Get Alerts by Severity
    public List<Alert> getAlertsBySeverity(String severity) {
        return alertRepo.findBySeverity(severity.toUpperCase());
    }

    // Get Alerts by Customer (used by the customer-facing Messages tab)
    public List<Alert> getAlertsByCustomerId(Long customerId) {
        return alertRepo.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    // API 4: Get Case by ID
    public CaseEntity getCaseById(String caseId) {
        return caseRepo.findById(caseId)
                .orElseThrow(() -> new CaseNotFoundException(caseId));
    }

    // API 5: Get Cases by Status
    public List<CaseEntity> getCasesByStatus(String status) {
        return caseRepo.findByCaseStatus(status.toUpperCase());
    }

    // API 6: Get Customer Details & Their Cases
    public CaseCustomer getCustomerWithCases(Long customerId) {
        return customerRepo.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));
    }

    // API 7: Get ONLY Cases for a Customer (Lighter payload)
    public List<CaseEntity> getCasesByCustomerId(Long customerId) {
        return caseRepo.findByCustomerCustomerId(customerId);
    }

    // API 8: Update Case Status (For Analyst Dashboard)
    @Transactional
    public CaseEntity updateCaseStatus(String caseId, String newStatus) {
        CaseEntity existingCase = getCaseById(caseId);
        existingCase.setCaseStatus(newStatus.toUpperCase());
        return caseRepo.save(existingCase);
    }

}
