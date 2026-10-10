package com.traceusage.traceusage.openapi.controller;

import com.traceusage.traceusage.openapi.exception.InvalidOpenApiImportException;
import com.traceusage.traceusage.shared.response.ApiResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = OpenApiImportController.class)
public class OpenApiImportExceptionHandler {

    @ExceptionHandler(InvalidOpenApiImportException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidOpenApiImport(
            InvalidOpenApiImportException exception) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(exception.getMessage()));
    }
}
