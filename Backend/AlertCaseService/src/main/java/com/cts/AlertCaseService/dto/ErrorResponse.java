package com.cts.AlertCaseService.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Standard error response structure for the AlertCaseService
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    
    private String errorCode;
    private String message;
    private String errorDetails;
    private String path;
    private int status;
    private LocalDateTime timestamp;
    
    public ErrorResponse(String errorCode, String message, int status) {
        this.errorCode = errorCode;
        this.message = message;
        this.status = status;
        this.timestamp = LocalDateTime.now();
    }
    
    public ErrorResponse(String errorCode, String message, String errorDetails, int status) {
        this.errorCode = errorCode;
        this.message = message;
        this.errorDetails = errorDetails;
        this.status = status;
        this.timestamp = LocalDateTime.now();
    }
}
