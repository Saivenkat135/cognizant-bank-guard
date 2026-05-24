package com.cts.apiGateway.config;

import com.cts.apiGateway.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import java.util.List;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
            "http://localhost:5173",
            "http://localhost:5174",
            "http://localhost:5175",
            "http://localhost:5176",
            "http://localhost:5177"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // Send a plain 401 (no WWW-Authenticate: Basic header) so browsers
                // don't pop the native sign-in dialog when a JWT is missing/invalid.
                .exceptionHandling(eh -> eh.authenticationEntryPoint((swe, e) -> {
                    swe.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return Mono.empty();
                }))
                .authorizeExchange(exchange -> exchange

                        // =============================================
                        // PUBLIC ENDPOINTS (no authentication required)
                        // =============================================
                        .pathMatchers(HttpMethod.POST, "/auth/login").permitAll()
                        .pathMatchers(HttpMethod.POST, "/auth/register").permitAll()

                        // =============================================
                        // USER MANAGEMENT (SuperAdmin only)
                        // =============================================
                        .pathMatchers("/auth/users/**").hasRole("SUPER_ADMIN")

                        // =============================================
                        // TRANSACTION SERVICE (/api/transactions/**)
                        // - Super Admin: full CRUD
                        // - Fraud Analyst: read-only (reviews transactions)
                        // - Risk Manager: read-only (reviews trends)
                        // =============================================
                        .pathMatchers(HttpMethod.GET, "/api/transactions/**")
                            .hasAnyRole("SUPER_ADMIN", "FRAUD_ANALYST", "RISK_MANAGER", "CUSTOMER")
                        .pathMatchers(HttpMethod.POST, "/api/transactions/**")
                            .hasRole("SUPER_ADMIN")
                        .pathMatchers(HttpMethod.PUT, "/api/transactions/**")
                            .hasRole("SUPER_ADMIN")
                        .pathMatchers(HttpMethod.DELETE, "/api/transactions/**")
                            .hasRole("SUPER_ADMIN")

                        // =============================================
                        // CUSTOMER SERVICE (/api/customers/**)
                        // - Super Admin: full CRUD
                        // - Fraud Analyst: read-only (view customer info during investigation)
                        // - Risk Manager: read-only (view customer risk profiles)
                        // =============================================
                        .pathMatchers(HttpMethod.GET, "/api/customers/**")
                            .hasAnyRole("SUPER_ADMIN", "FRAUD_ANALYST", "RISK_MANAGER", "CUSTOMER")
                        .pathMatchers(HttpMethod.POST, "/api/customers/**")
                            .hasRole("SUPER_ADMIN")
                        .pathMatchers(HttpMethod.PUT, "/api/customers/**")
                            .hasRole("SUPER_ADMIN")
                        .pathMatchers(HttpMethod.DELETE, "/api/customers/**")
                            .hasRole("SUPER_ADMIN")

                        // =============================================
                        // ALERT & CASE INVESTIGATION (/api/investigation/**)
                        // - Super Admin: full access
                        // - Fraud Analyst: read alerts, view cases, update case status
                        // - Risk Manager: no access
                        // =============================================
                        .pathMatchers(HttpMethod.GET, "/api/investigation/**")
                            .hasAnyRole("SUPER_ADMIN", "FRAUD_ANALYST")
                        .pathMatchers(HttpMethod.PUT, "/api/investigation/cases/*/status")
                            .hasAnyRole("SUPER_ADMIN", "FRAUD_ANALYST")
                        .pathMatchers(HttpMethod.POST, "/api/investigation/**")
                            .hasRole("SUPER_ADMIN")

                        // =============================================
                        // SAR REPORTS (/sar/**)
                        // - Super Admin: full access
                        // - Fraud Analyst: read-only (reviews SAR reports)
                        // - Risk Manager: no access
                        // =============================================
                        .pathMatchers(HttpMethod.GET, "/sar/**")
                            .hasAnyRole("SUPER_ADMIN", "FRAUD_ANALYST")
                        .pathMatchers(HttpMethod.POST, "/sar/**")
                            .hasRole("SUPER_ADMIN")

                        // =============================================
                        // ENRICHMENT SERVICE (/api/enrich/**)
                        // - Super Admin: full access
                        // - Risk Manager: defines rules, thresholds, risk policies
                        // - Fraud Analyst: no access
                        // =============================================
                        .pathMatchers("/api/enrich/**")
                            .hasAnyRole("SUPER_ADMIN", "RISK_MANAGER")

                        // =============================================
                        // DECISION ENGINE / GEMINI (/api/gemini/**)
                        // - Super Admin: full access
                        // - Risk Manager: defines detection rules & risk policies
                        // - Fraud Analyst: no access
                        // =============================================
                        .pathMatchers("/api/gemini/**")
                            .hasAnyRole("SUPER_ADMIN", "RISK_MANAGER")

                        // =============================================
                        // ALL OTHER ENDPOINTS - must be authenticated
                        // =============================================
                        .anyExchange().authenticated()
                )
                // Add JWT filter before the authentication filter
                .addFilterAt(jwtAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }
}
