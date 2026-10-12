package com.traceusage.traceusage.messaging.exception;

public class KafkaIngestionException extends RuntimeException {

    public KafkaIngestionException(String message, Throwable cause) {
        super(message, cause);
    }
}
