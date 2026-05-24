package com.cts.AlertCaseService.exception;

/**
 * Exception thrown when a Case is not found in the database
 */
public class CaseNotFoundException extends AlertException {
    
    public CaseNotFoundException(String caseId) {
        super("Case not found with ID: " + caseId, "CASE_NOT_FOUND");
    }
    
    public CaseNotFoundException(String caseId, String message) {
        super(message, "CASE_NOT_FOUND");
    }
}
