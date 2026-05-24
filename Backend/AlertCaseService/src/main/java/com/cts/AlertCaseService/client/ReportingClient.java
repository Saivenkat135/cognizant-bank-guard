package com.cts.AlertCaseService.client;

import com.cts.AlertCaseService.dto.ReportingRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class ReportingClient {

    private final WebClient webClient;

    @Value("${external.reporting-service.url}")
    private String reportingServiceUrl;

    public ReportingClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public void sendToReporting(ReportingRequest request) {
        try {
            log.info("Sending report to ReportingService at: {}", reportingServiceUrl);
            log.debug("Report details - Case ID: {}, Risk Score: {}, Decision: {}", 
                    request.getCaseId(), request.getRiskScore(), request.getGeminiDecision());
            
            Mono<Void> response = webClient
                    .post()
                    .uri(reportingServiceUrl)
                    .bodyValue(request)
                    .retrieve()
                    .toBodilessEntity()
                    .doOnSuccess(entity -> {
                        log.info("✓ Report sent successfully to ReportingService");
                        System.out.println("✓ Successfully sent data to Reporting Service");
                    })
                    .doOnError(error -> {
                        log.error("✗ Failed to send report to ReportingService: {}", error.getMessage());
                        System.err.println("✗ Failed to send data to Reporting Service: " + error.getMessage());
                    })
                    .then();
            
            // Block to ensure synchronous execution
            response.block();
            
        } catch (Exception e) {
            log.error("Exception while sending report to ReportingService", e);
            System.err.println("✗ Exception sending to Reporting Service: " + e.getMessage());
        }
    }
}

