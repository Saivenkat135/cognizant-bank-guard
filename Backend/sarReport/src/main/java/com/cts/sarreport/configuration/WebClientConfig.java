package com.cts.sarreport.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
    @Bean
    public WebClient webClient() {
        // Use the static create() or builder() method directly
        return WebClient.builder()
                .baseUrl("http://external-microservice-url")
                .build();
    }
}