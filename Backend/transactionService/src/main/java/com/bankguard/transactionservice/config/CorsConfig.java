package com.bankguard.transactionservice.config;
 // change to your actual package

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowCredentials(true);
        config.addAllowedOrigin("http://localhost:3000");  // customer-frontend dev server
        config.addAllowedOrigin("http://localhost:5174");  // legacy / other React dev server
        config.addAllowedHeader("*");                      // allow all headers
        config.addAllowedMethod("*");                      // GET, POST, PUT, DELETE, etc.

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);   // apply to all endpoints

        return new CorsFilter(source);
    }
}