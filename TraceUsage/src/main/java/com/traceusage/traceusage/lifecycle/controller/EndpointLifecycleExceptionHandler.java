package com.traceusage.traceusage.lifecycle.controller;

import com.traceusage.traceusage.lifecycle.exception.EndpointNotFoundException;
import com.traceusage.traceusage.lifecycle.exception.InvalidDeprecationException;
import com.traceusage.traceusage.shared.response.ApiResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = EndpointLifecycleController.class)
public class EndpointLifecycleExceptionHandler {

    @ExceptionHandler(EndpointNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleEndpointNotFound(
            EndpointNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(exception.getMessage()));
    }

    @ExceptionHandler(InvalidDeprecationException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidDeprecation(
            InvalidDeprecationException exception) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(exception.getMessage()));
    }
}