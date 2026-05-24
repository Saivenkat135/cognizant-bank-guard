package com.cts.apiGateway.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    private String username;
    private String password;
    private String role; // SUPER_ADMIN, FRAUD_ANALYST, RISK_MANAGER
}
