package com.cts.AlertCaseService.exception;

/**
 * Exception thrown when a Customer is not found in the database
 */
public class CustomerNotFoundException extends AlertException {
    
    public CustomerNotFoundException(Long customerId) {
        super("Customer not found with ID: " + customerId, "CUSTOMER_NOT_FOUND");
    }
    
    public CustomerNotFoundException(String customerId, String message) {
        super(message, "CUSTOMER_NOT_FOUND");
    }
}
