package com.demo.controller;

import com.demo.service.DemoStore;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController @CrossOrigin(origins = "http://localhost:3001") @RequestMapping("/api/categories")
public class CatalogController {
    private final DemoStore store;
    public CatalogController(DemoStore store) { this.store = store; }
    @GetMapping public List<DemoStore.CategoryResponse> categories() { return store.categories(); }
    @GetMapping("/{id}") public DemoStore.CategoryResponse category(@PathVariable Long id) { return store.category(id); }
    @GetMapping("/{id}/products") public List<DemoStore.ProductSummary> products(@PathVariable Long id) { return store.productsByCategory(id); }
}
