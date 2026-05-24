package com.cts.sarreport.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI sarServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SAR Reporting API")
                        .description("API for managing Suspicious Activity Reports and Compliance")
                        .version("1.0.0"));
    }
}