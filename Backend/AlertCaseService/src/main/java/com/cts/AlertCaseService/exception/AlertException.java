package com.cts.AlertCaseService.exception;

/**
 * Base custom exception for AlertCaseService
 * All AlertCaseService specific exceptions will extend this class
 */
public class AlertException extends RuntimeException {
    
    private String errorCode;
    protected String errorDetails;
    
    public AlertException(String message) {
        super(message);
        this.errorCode = "ALERT_ERROR";
    }
    
    public AlertException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public AlertException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "ALERT_ERROR";
    }
    
    public AlertException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public AlertException(String message, String errorCode, String errorDetails) {
        super(message);
        this.errorCode = errorCode;
        this.errorDetails = errorDetails;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public String getErrorDetails() {
        return errorDetails;
    }
}
