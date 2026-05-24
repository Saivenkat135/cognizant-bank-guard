//package com.bankguard.enrichmentservice.config;
//
//import org.springframework.cloud.client.loadbalancer.LoadBalanced;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.web.reactive.function.client.WebClient;
//
//@Configuration
//public class WebTemplateConfig {
//
//    @Bean
//    @LoadBalanced
//    public WebClient.Builder webClientBuilder() {
//        return WebClient.builder();
//    }
//}
//
//


package com.bankguard.enrichmentservice.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration // CRITICAL: Tells Spring to scan this file for beans
public class WebTemplateConfig {

    // CRITICAL: The name here must EXACTLY match what is inside your @Qualifier
    @Bean(name = "loadBalancedWebClient")
    @LoadBalanced
    @Primary
    public WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder();
    }

    // Standard client for regular internet/localhost calls
    @Bean(name = "standardWebClient")
    public WebClient.Builder standardWebClientBuilder() {
        return WebClient.builder();
    }
}