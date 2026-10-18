package com.demo.controller;

import com.demo.service.DemoStore;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:3001")
@RequestMapping("/api/products")
public class ProductController {
    private final DemoStore store;
    public ProductController(DemoStore store) { this.store = store; }
    @GetMapping public List<DemoStore.ProductSummary> products() { return store.products(); }
    @GetMapping("/{id}") public DemoStore.ProductResponse product(@PathVariable Long id) { return store.product(id); }
    @PostMapping public ResponseEntity<DemoStore.ProductResponse> create(@Valid @RequestBody DemoStore.CreateProductRequest request) { return ResponseEntity.status(HttpStatus.CREATED).body(store.create(request)); }
    @PatchMapping("/{id}") public DemoStore.ProductResponse update(@PathVariable Long id, @Valid @RequestBody DemoStore.UpdateProductRequest request) { return store.update(id, request); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long id) { store.delete(id); return ResponseEntity.noContent().build(); }
    @GetMapping("/{id}/reviews") public DemoStore.ReviewsResponse reviews(@PathVariable Long id) { return store.reviews(id); }
}
