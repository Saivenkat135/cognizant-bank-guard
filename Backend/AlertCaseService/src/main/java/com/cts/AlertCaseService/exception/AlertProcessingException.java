package com.cts.AlertCaseService.exception;

/**
 * Exception thrown when there is an error processing fraud alerts
 */
public class AlertProcessingException extends AlertException {
    
    public AlertProcessingException(String message) {
        super(message, "ALERT_PROCESSING_ERROR");
    }
    
    public AlertProcessingException(String message, Throwable cause) {
        super(message, "ALERT_PROCESSING_ERROR", cause);
    }
    
    public AlertProcessingException(String message, String details, Throwable cause) {
        super(message, "ALERT_PROCESSING_ERROR", cause);
        this.errorDetails = details;
    }
}
