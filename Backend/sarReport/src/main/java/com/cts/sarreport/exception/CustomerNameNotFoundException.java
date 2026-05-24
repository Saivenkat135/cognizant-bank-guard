package com.cts.sarreport.exception;

public class CustomerNameNotFoundException extends RuntimeException{
    public CustomerNameNotFoundException(String name) {
        super("No customer records found matching the name: " + name);
    }
}
