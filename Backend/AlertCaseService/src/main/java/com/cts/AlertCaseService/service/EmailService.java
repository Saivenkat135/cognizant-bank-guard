package com.cts.AlertCaseService.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class EmailService {

    private final RestClient restClient = RestClient.create();

    @Value("${brevo.api-key:}")
    private String apiKey;

    @Value("${brevo.from:}")
    private String fromAddress;

    @Value("${brevo.from-name:BankGuard}")
    private String fromName;

    @Value("${brevo.api-url:https://api.brevo.com/v3/smtp/email}")
    private String apiUrl;

    public void sendEmail(String to, String subject, String body) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "brevo.api-key is not set. Configure it in application.yml " +
                    "or set the BREVO_API_KEY environment variable.");
        }
        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("Recipient email address is missing.");
        }

        Map<String, Object> payload = Map.of(
                "sender", Map.of("name", fromName, "email", fromAddress),
                "to", List.of(Map.of("email", to)),
                "subject", subject,
                "textContent", body
        );

        restClient.post()
                .uri(apiUrl)
                .header("api-key", apiKey)
                .header("accept", "application/json")
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .toBodilessEntity();
    }
}
