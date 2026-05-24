package com.bankguard.transactionservice.exception;

/**
 * Exception thrown when transaction processing fails
 */
public class TransactionProcessingException extends RuntimeException {
    
    private String errorCode;
    
    public TransactionProcessingException(String message) {
        super(message);
        this.errorCode = "TRANSACTION_PROCESSING_ERROR";
    }
    
    public TransactionProcessingException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public TransactionProcessingException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "TRANSACTION_PROCESSING_ERROR";
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}
