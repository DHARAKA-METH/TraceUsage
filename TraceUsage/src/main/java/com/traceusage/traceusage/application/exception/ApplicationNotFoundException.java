package com.traceusage.traceusage.application.exception;

public class ApplicationNotFoundException extends RuntimeException {

    public ApplicationNotFoundException() {
        super("Application not found");
    }
}
