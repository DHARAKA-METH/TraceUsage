package com.traceusage.traceusage.lifecycle.exception;

public class EndpointNotFoundException extends RuntimeException {

    public EndpointNotFoundException() {
        super("Endpoint not found");
    }
}