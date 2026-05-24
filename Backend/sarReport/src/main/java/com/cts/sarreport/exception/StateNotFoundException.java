package com.cts.sarreport.exception;

public class StateNotFoundException extends RuntimeException {
    public StateNotFoundException(String state) {
        super("No SAR data available for the state: " + state);
    }
}
