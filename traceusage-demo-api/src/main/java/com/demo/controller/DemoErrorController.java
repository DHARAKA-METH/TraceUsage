package com.demo.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController @CrossOrigin(origins = "http://localhost:3001") @RequestMapping("/api/demo/errors")
public class DemoErrorController {
    @GetMapping("/400") public void badRequest() { throw new DemoHttpException(HttpStatus.BAD_REQUEST, "Intentional demo 400 response"); }
    @GetMapping("/500") public void serverError() { throw new DemoHttpException(HttpStatus.INTERNAL_SERVER_ERROR, "Intentional demo 500 response"); }
    public record ErrorResponse(int status, String message) { }
    public static class DemoHttpException extends RuntimeException { final HttpStatus status; DemoHttpException(HttpStatus status, String message) { super(message); this.status = status; } }
}
