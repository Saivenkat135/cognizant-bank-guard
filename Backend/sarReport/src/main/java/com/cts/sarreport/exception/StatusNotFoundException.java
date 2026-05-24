package com.cts.sarreport.exception;

public class StatusNotFoundException extends RuntimeException {
    public StatusNotFoundException(String status) {
        super("The report status '" + status + "' is invalid or has no associated records.");
    }
}
