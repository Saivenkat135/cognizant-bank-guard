package com.cts.sarreport.exception;

public class CustomerAccountNoNotFoundException extends RuntimeException {
    public CustomerAccountNoNotFoundException(String accountNo) {
        super("Account number " + accountNo + " does not exist in our reporting records.");
    }
}
