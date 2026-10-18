package com.demo.controller;

import com.demo.service.DemoStore;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController @CrossOrigin(origins = "http://localhost:3001") @RequestMapping("/api/orders")
public class OrderController {
    private final DemoStore store;
    public OrderController(DemoStore store) { this.store = store; }
    @PostMapping public ResponseEntity<DemoStore.OrderResponse> create(@Valid @RequestBody DemoStore.CreateOrderRequest request) { return ResponseEntity.status(HttpStatus.CREATED).body(store.createOrder(request)); }
    @GetMapping("/{id}") public DemoStore.OrderResponse order(@PathVariable String id) { return store.order(id); }
}
