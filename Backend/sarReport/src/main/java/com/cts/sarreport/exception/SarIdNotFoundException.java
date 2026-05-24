package com.cts.sarreport.exception;

public class SarIdNotFoundException extends RuntimeException{
    public SarIdNotFoundException(int id) {
        super("SAR Report with ID " + id + " was not found in the system.");
    }
}
