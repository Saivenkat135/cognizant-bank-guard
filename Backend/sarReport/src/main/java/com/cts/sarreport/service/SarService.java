package com.cts.sarreport.service;

import com.cts.sarreport.dto.ReportingRequest;
import com.cts.sarreport.entity.SarReport;
import com.cts.sarreport.exception.*;
import com.cts.sarreport.repository.SarRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class SarService{

    @Autowired
    private SarRepository sarRepository;

    @Autowired
    private WebClient webClient;


//    public SarReport putDetails(SarReport report) {
//
//        SarReport externalData = webClient.get()
//                .uri("/alert-cases/transaction/{id}", report.getTransactionId())
//                .retrieve()
//                .bodyToMono(SarReport.class)
//                .block();
//
//        if (externalData != null) {
//            // 2. Merge external data into your local report object
//            report.setCustomerName(externalData.getCustomerName());
//            report.setCustomerEmail(externalData.getCustomerEmail());
//            report.setCustomerAccountNo(externalData.getCustomerAccountNo());
//            report.setCity(externalData.getCity());
//            report.setState(externalData.getState());
//            report.setAmount(externalData.getAmount());
//            report.setTime(externalData.getTime());
//        }
//
//        report.setLocalDate(new Date());
//        return sarRepository.save(report);
// }


    public SarReport generateById(int sarId) {
        return sarRepository.findById(sarId)
                .orElseThrow(() -> new SarIdNotFoundException(sarId));
    }

    public SarReport generateByName(String customerName) {
        return sarRepository.findByCustomerName(customerName)
                .orElseThrow(() -> new CustomerNameNotFoundException(customerName));
    }

    public SarReport generateByAccountNo(String customerAccountNo) {
        return sarRepository.findByCustomerAccountNo(customerAccountNo)
                .orElseThrow(() -> new CustomerAccountNoNotFoundException(customerAccountNo));
    }

    public List<SarReport> generateByStatus(String status) {
        List<SarReport> reports = sarRepository.findByStatus(status);
        if (reports.isEmpty()) {
            throw new StatusNotFoundException(status);
        }
        return reports;
    }

//    public SarReport generateByTransactionId(Long transactionId) {
//        SarReport report = sarRepository.findByTransactionId(transactionId)
//                .orElseThrow(() -> new TransactionIdNotFoundException(transactionId));
//
//        report.setLocalDate(new Date());
//        return sarRepository.save(report);
//    }

    public List<SarReport> generateByCity(String city) {
        List<SarReport> reports = sarRepository.findByCity(city);
        if (reports.isEmpty()) {
            throw new CityNotFoundException(city);
        }
        return reports;
    }

    public List<SarReport> generateByState(String state) {
        List<SarReport> reports = sarRepository.findByState(state);
        if (reports.isEmpty()) {
            throw new StateNotFoundException(state);
        }
        return reports;
    }

    public List<SarReport> getAllReports() {
        return sarRepository.findAll();
    }

    // ==========================================
    // ALERT CASE SERVICE INTEGRATION METHODS
    // ==========================================
    /**
     * Process ReportingRequest from AlertCaseService
     * Convert it to SarReport and store it
     * Extracts customerPayload enrichment data and maps it to SarReport fields
     */
    public SarReport processReportingRequest(ReportingRequest reportingRequest) {
        log.info("Processing ReportingRequest from AlertCaseService - Case ID: {}", reportingRequest.getCaseId());

        try {
            // Create a new SarReport from ReportingRequest
            SarReport sarReport = new SarReport();
            
            // Map fields from ReportingRequest to SarReport
            sarReport.setCaseId(reportingRequest.getCaseId());
            sarReport.setCustomerId(reportingRequest.getCustomerId());
            sarReport.setStatus(reportingRequest.getStatus());
            sarReport.setRiskScore(reportingRequest.getRiskScore());
            sarReport.setReason(reportingRequest.getReason());
            sarReport.setAmount(reportingRequest.getAmount());
            sarReport.setCity(reportingRequest.getCity());
            sarReport.setState(reportingRequest.getState());
            sarReport.setTime(reportingRequest.getTime());
            sarReport.setCustomerName(reportingRequest.getCustomerName());
            sarReport.setCustomerEmail(reportingRequest.getCustomerEmail());
            sarReport.setCustomerAccountNo(reportingRequest.getCustomerAccountNo());
            sarReport.setLocalDate(new Date());

            // Extract and map enrichment data from customerPayload
//            if (reportingRequest.getCustomerPayload() != null) {
//                log.debug("Extracting enrichment data from customerPayload");
//                extractAndMapCustomerPayload(sarReport, reportingRequest.getCustomerPayload());
//                log.debug("✓ Enrichment data extracted and mapped");
//            } else {
//                log.debug("No customerPayload provided - using only ReportingRequest direct fields");
//            }
            
            // Save the SarReport to database
            SarReport savedReport = sarRepository.save(sarReport);
            
            log.info("✓ SAR Report created successfully - SAR ID: {}, Case ID: {}", 
                    savedReport.getSarId(), reportingRequest.getCaseId());
            log.debug("SAR Report fields - City: {}, State: {}, Email: {}, AccountNo: {}, Time: {}", 
                    savedReport.getCity(), savedReport.getState(), savedReport.getCustomerEmail(), 
                    savedReport.getCustomerAccountNo(), savedReport.getTime());
            System.out.println("✓ SAR Report stored successfully with ID: " + savedReport.getSarId());
            
            return savedReport;
            
        } catch (Exception e) {
            log.error("✗ Error processing ReportingRequest: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process reporting request: " + e.getMessage(), e);
        }
    }
    
    /**
     * Extract enrichment data from customerPayload and map to SarReport
     * CustomerPayload can be a Map or another object with enrichment details
     * 
     * Expected fields in customerPayload:
     * - city, state, customerEmail, customerAccountNo, time, transactionId, amount, etc.
     */
    private void extractAndMapCustomerPayload(SarReport sarReport, Object customerPayload) {
        try {
            if (customerPayload instanceof java.util.Map) {
                // Handle Map type payload
                java.util.Map<String, Object> payloadMap = (java.util.Map<String, Object>) customerPayload;
                
                // Extract city
                if (payloadMap.containsKey("city")) {
                    Object city = payloadMap.get("city");
                    if (city != null && !city.toString().equalsIgnoreCase("NULL")) {
                        sarReport.setCity(city.toString());
                        log.debug("Mapped city: {}", city);
                    }
                }
                
                // Extract state
                if (payloadMap.containsKey("state")) {
                    Object state = payloadMap.get("state");
                    if (state != null && !state.toString().equalsIgnoreCase("NULL")) {
                        sarReport.setState(state.toString());
                        log.debug("Mapped state: {}", state);
                    }
                }
                
                // Extract customerEmail
                if (payloadMap.containsKey("customerEmail") || payloadMap.containsKey("email")) {
                    Object email = payloadMap.getOrDefault("customerEmail", payloadMap.get("email"));
                    if (email != null && !email.toString().equalsIgnoreCase("NULL")) {
                        sarReport.setCustomerEmail(email.toString());
                        log.debug("Mapped customerEmail: {}", email);
                    }
                }
                
                // Extract customerAccountNo
                if (payloadMap.containsKey("customerAccountNo") || payloadMap.containsKey("accountNo")) {
                    Object accountNo = payloadMap.getOrDefault("customerAccountNo", payloadMap.get("accountNo"));
                    if (accountNo != null && !accountNo.toString().equalsIgnoreCase("NULL")) {
                        sarReport.setCustomerAccountNo(accountNo.toString());
                        log.debug("Mapped customerAccountNo: {}", accountNo);
                    }
                }
                
                // Extract time
                if (payloadMap.containsKey("time")) {
                    Object time = payloadMap.get("time");
                    if (time != null && !time.toString().equalsIgnoreCase("NULL")) {
                        try {
                            if (time instanceof java.time.LocalDateTime) {
                                sarReport.setTime((java.time.LocalDateTime) time);
                            } else if (time instanceof String) {
                                // Try to parse String to LocalDateTime
                                sarReport.setTime(java.time.LocalDateTime.parse(time.toString()));
                            } else if (time instanceof Long) {
                                // Convert timestamp to LocalDateTime
                                sarReport.setTime(
                                    java.time.LocalDateTime.ofInstant(
                                        java.time.Instant.ofEpochMilli((Long) time),
                                        java.time.ZoneId.systemDefault()
                                    )
                                );
                            }
                            log.debug("Mapped time: {}", time);
                        } catch (Exception e) {
                            log.warn("Could not parse time field: {}, Error: {}", time, e.getMessage());
                        }
                    }
                }
                
                // Extract transactionId (override if provided in payload)
                if (payloadMap.containsKey("transactionId")) {
                    Object txId = payloadMap.get("transactionId");
                    if (txId != null && !txId.toString().equalsIgnoreCase("NULL")) {
                        try {
                            log.debug("Mapped transactionId from payload: {}", txId);
                        } catch (NumberFormatException e) {
                            log.warn("Could not parse transactionId: {}", txId);
                        }
                    }
                }
                
                // Extract amount (override if provided in payload)
                if (payloadMap.containsKey("amount")) {
                    Object amt = payloadMap.get("amount");
                    if (amt != null && !amt.toString().equalsIgnoreCase("NULL")) {
                        try {
                            sarReport.setAmount(Double.valueOf(amt.toString()));
                            log.debug("Mapped amount from payload: {}", amt);
                        } catch (NumberFormatException e) {
                            log.warn("Could not parse amount: {}", amt);
                        }
                    }
                }
                
                log.info("✓ Successfully extracted enrichment data from Map payload");
            } else {
                // Handle other object types - try to use reflection or convert to Map
                log.debug("CustomerPayload is not a Map, attempting to convert...");
                
                // Try to convert using Jackson ObjectMapper (if available)
                try {
                    java.util.Map<String, Object> convertedMap = 
                        new com.fasterxml.jackson.databind.ObjectMapper()
                            .convertValue(customerPayload, java.util.Map.class);
                    
                    log.debug("Successfully converted object to Map, extracting fields...");
                    extractAndMapCustomerPayload(sarReport, convertedMap);
                } catch (Exception e) {
                    log.warn("Could not convert customerPayload to Map: {}", e.getMessage());
                    // Continue without enrichment data
                }
            }
        } catch (Exception e) {
            log.error("Error extracting customer payload: {}", e.getMessage(), e);
            // Don't fail the entire request, just log the error and continue
        }
    }
}
