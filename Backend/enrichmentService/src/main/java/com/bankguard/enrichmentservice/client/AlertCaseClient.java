//package com.bankguard.enrichmentservice.client;
//
//import com.bankguard.enrichmentservice.dto.AlertCasePayload;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Component;
//import org.springframework.web.reactive.function.client.WebClient;
//import reactor.core.publisher.Mono;
//import java.time.Duration;
//
//@Component
//@Slf4j
//public class AlertCaseClient {
//
//    private final WebClient webClient;
//
//    @Value("${external.alertcase-service.url:http://localhost:8085}")
//    private String alertCaseServiceUrl;
//
//    public AlertCaseClient(WebClient.Builder webClientBuilder) {
//        this.webClient = webClientBuilder.build();
//    }
//
//    public void sendToAlertCase(AlertCasePayload payload) {
//        try {
//            String endpoint = alertCaseServiceUrl + "/api/investigation/ingest-fraud-alert";
//            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
//            log.info("SENDING FRAUD ALERT TO ALERTCASE SERVICE");
//            log.info("Endpoint: {}", endpoint);
//            log.info("Decision: {}, Risk Score: {}, Transaction ID: {}",
//                    payload.getDecisionStatus(),
//                    payload.getGeminiRiskScore());
//            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
//
//            Mono<Void> response = webClient
//                    .post()
//                    .uri(endpoint)
//                    .bodyValue(payload)
//                    .retrieve()
//                    .toBodilessEntity()
//                    .doOnSuccess(entity -> {
//                        log.info("✓ SUCCESS: Fraud alert received by AlertCaseService");
//                        log.info("✓ Risk Score: {}, Decision: {}",
//                                payload.getGeminiRiskScore(), payload.getDecisionStatus());
//                        System.out.println("✓ Fraud alert successfully sent to AlertCaseService");
//                    })
//                    .doOnError(error -> {
//                        log.error("✗ FAILED: Could not reach AlertCaseService at {}", endpoint);
//                        log.error("✗ Error Details: {} - {}", error.getClass().getSimpleName(), error.getMessage());
//                        System.err.println("✗ Failed to send fraud alert: " + error.getMessage());
//                    })
//                    .then();
//
//            // Block to ensure synchronous execution with 10 second timeout
//            response.timeout(Duration.ofSeconds(10)).block();
//
//        } catch (Exception e) {
//            log.error("✗ Exception while sending alert to AlertCaseService: {}", e.getClass().getSimpleName());
//            log.error("✗ Error message: {}", e.getMessage());
//            log.error("✗ Stack trace: ", e);
//            System.err.println("✗ Exception: " + e.getMessage());
//            throw new RuntimeException("Failed to send alert to AlertCaseService: " + e.getMessage(), e);
//        }
//    }
//}
//


package com.bankguard.enrichmentservice.client;

import com.bankguard.enrichmentservice.dto.AlertCasePayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@Slf4j
public class AlertCaseClient {

    private final WebClient webClient;

    @Autowired
    public AlertCaseClient(@Qualifier("loadBalancedWebClient") WebClient.Builder webClientBuilder) {
        // IMPORTANT: You must use the injected builder to create the WebClient
        // Do NOT use WebClient.create() here.
        this.webClient = webClientBuilder.baseUrl("http://alert-case-service").build();
    }

    public void sendToAlertCase(AlertCasePayload payload) {
        webClient.post()
                .uri("/api/investigation/ingest-fraud-alert") // Just the path, baseUrl is set above
                .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }
}