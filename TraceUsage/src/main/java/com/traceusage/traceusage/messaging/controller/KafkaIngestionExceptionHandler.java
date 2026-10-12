package com.traceusage.traceusage.messaging.controller;

import com.traceusage.traceusage.messaging.exception.KafkaIngestionException;
import com.traceusage.traceusage.shared.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class KafkaIngestionExceptionHandler {

    @ExceptionHandler(KafkaIngestionException.class)
    public ResponseEntity<ApiResponse<Void>> handleKafkaIngestionFailure(
            KafkaIngestionException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error(exception.getMessage()));
    }
}
