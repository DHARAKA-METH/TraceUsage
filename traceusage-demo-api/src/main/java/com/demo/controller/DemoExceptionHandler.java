package com.demo.controller;

import com.demo.service.DemoStore;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class DemoExceptionHandler {
    @ExceptionHandler(DemoStore.NotFoundException.class)
    ResponseEntity<DemoErrorController.ErrorResponse> notFound(DemoStore.NotFoundException exception) { return response(HttpStatus.NOT_FOUND, exception.getMessage()); }
    @ExceptionHandler(DemoErrorController.DemoHttpException.class)
    ResponseEntity<DemoErrorController.ErrorResponse> intentional(DemoErrorController.DemoHttpException exception) { return response(exception.status, exception.getMessage()); }
    private ResponseEntity<DemoErrorController.ErrorResponse> response(HttpStatus status, String message) { return ResponseEntity.status(status).body(new DemoErrorController.ErrorResponse(status.value(), message)); }
}
