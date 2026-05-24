package com.bankguard.transactionservice.exception;

/**
 * Exception thrown when receiver's account number is not found in the system
 */
public class ReceiverAccountNotFoundException extends RuntimeException {
    
    private String accountNumber;
    
    public ReceiverAccountNotFoundException(String accountNumber) {
        super("Receiver account not found: " + accountNumber);
        this.accountNumber = accountNumber;
    }
    
    public ReceiverAccountNotFoundException(String message, String accountNumber) {
        super(message);
        this.accountNumber = accountNumber;
    }
    
    public String getAccountNumber() {
        return accountNumber;
    }
}
