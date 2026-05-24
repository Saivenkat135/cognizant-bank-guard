package com.cts.AlertCaseService.exception;

/**
 * Exception thrown when the incoming payload is invalid or malformed
 */
public class InvalidPayloadException extends AlertException {
    
    public InvalidPayloadException(String message) {
        super(message, "INVALID_PAYLOAD");
    }
    
    public InvalidPayloadException(String message, String details) {
        super(message, "INVALID_PAYLOAD", details);
    }
    
    public InvalidPayloadException(String message, Throwable cause) {
        super(message, "INVALID_PAYLOAD", cause);
    }
}
