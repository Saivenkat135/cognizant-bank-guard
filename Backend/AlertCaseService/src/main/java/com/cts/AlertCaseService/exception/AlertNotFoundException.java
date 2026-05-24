package com.cts.AlertCaseService.exception;

/**
 * Exception thrown when an Alert is not found in the database
 */
public class AlertNotFoundException extends AlertException {
    
    public AlertNotFoundException(String alertId) {
        super("Alert not found with ID: " + alertId, "ALERT_NOT_FOUND");
    }
    
    public AlertNotFoundException(String alertId, String message) {
        super(message, "ALERT_NOT_FOUND");
    }
}
