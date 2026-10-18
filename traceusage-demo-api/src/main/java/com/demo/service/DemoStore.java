package com.demo.service;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/** Deterministic in-memory data for the TraceUsage telemetry demo. */
@Service
public class DemoStore {

    private final Map<Long, ProductResponse> products = new LinkedHashMap<>();
    private final AtomicLong productIds = new AtomicLong(103);
    private final AtomicLong orderIds = new AtomicLong(7001);

    public DemoStore() {
        products.put(101L, product(101L, "Laptop", 250000, 14, "LAP-001", "Acme Electronics"));
        products.put(102L, product(102L, "Wireless Headphones", 18500, 28, "AUD-002", "Soundwave Labs"));
        products.put(103L, product(103L, "Mechanical Keyboard", 22000, 8, "KEY-003", "Keycraft"));
    }

    public List<ProductSummary> products() {
        return products.values().stream()
                .map(product -> new ProductSummary(product.id(), product.name(), product.price(), product.currency(),
                        product.imageUrl(), product.stock(), product.tags(), product.supplier()))
                .toList();
    }

    public ProductResponse product(Long id) {
        ProductResponse product = products.get(id);
        if (product == null) throw new NotFoundException("Product " + id + " was not found");
        return product;
    }

    public ProductResponse create(CreateProductRequest request) {
        long id = productIds.incrementAndGet();
        ProductResponse product = product(id, request.name(), request.price(), request.stock(), "LAP-NEW-" + id, "Acme Electronics");
        products.put(id, product);
        return product;
    }

    public ProductResponse update(Long id, UpdateProductRequest request) {
        ProductResponse current = product(id);
        ProductResponse updated = new ProductResponse(
                current.id(), request.name() == null ? current.name() : request.name(),
                request.price() == null ? current.price() : request.price(), current.currency(),
                request.description() == null ? current.description() : request.description(), current.imageUrl(),
                request.stock() == null ? current.stock() : request.stock(), current.legacyCode(),
                current.internalCatalogReference(), current.tags(), current.supplier(), current.specifications());
        products.put(id, updated);
        return updated;
    }

    public void delete(Long id) {
        if (products.remove(id) == null) throw new NotFoundException("Product " + id + " was not found");
    }

    public List<CategoryResponse> categories() {
        return List.of(new CategoryResponse(1L, "Computers", "Laptops and accessories"),
                new CategoryResponse(2L, "Audio", "Headphones and speakers"));
    }

    public CategoryResponse category(Long id) {
        return categories().stream().filter(category -> category.id().equals(id)).findFirst()
                .orElseThrow(() -> new NotFoundException("Category " + id + " was not found"));
    }

    public List<ProductSummary> productsByCategory(Long id) {
        category(id);
        return id == 2L ? products().stream().filter(product -> product.tags().contains("audio")).toList() : products();
    }

    public ReviewsResponse reviews(Long productId) {
        product(productId);
        return new ReviewsResponse(productId, List.of(
                new ReviewResponse("rev-1", 5, "Very fast and reliable", new Reviewer("Alice Example", "alice@example.invalid")),
                new ReviewResponse("rev-2", 4, "Excellent display", new Reviewer("Sam Example", "sam@example.invalid"))));
    }

    public OrderResponse createOrder(CreateOrderRequest request) {
        List<OrderItem> items = new ArrayList<>();
        for (OrderLineRequest line : request.items()) {
            ProductResponse product = product(line.productId());
            items.add(new OrderItem(product.id(), product.name(), line.quantity(), product.price(), product.price() * line.quantity()));
        }
        int total = items.stream().mapToInt(OrderItem::lineTotal).sum();
        return new OrderResponse("ord-" + orderIds.incrementAndGet(), "CONFIRMED", total, "LKR", items,
                new ShippingAddress("Colombo", "Sri Lanka", "LK-001"), "Internal fulfilment note");
    }

    public OrderResponse order(String id) {
        return new OrderResponse(id, "CONFIRMED", 250000, "LKR",
                List.of(new OrderItem(101L, "Laptop", 1, 250000, 250000)),
                new ShippingAddress("Colombo", "Sri Lanka", "LK-001"), "Internal fulfilment note");
    }

    private ProductResponse product(long id, String name, int price, int stock, String legacyCode, String supplierName) {
        return new ProductResponse(id, name, price, "LKR", "A reliable " + name + " for work and play.",
                "https://images.example.invalid/products/" + id + ".png", stock, legacyCode,
                "CAT-INT-" + id, List.of("computers", id == 102 ? "audio" : "featured"),
                new Supplier(id + 900, supplierName, "Sri Lanka", 4.7),
                new Specifications("Intel Core i7", 16, 512));
    }

    public record ProductResponse(Long id, String name, Integer price, String currency, String description,
                                  String imageUrl, Integer stock, String legacyCode, String internalCatalogReference,
                                  List<String> tags, Supplier supplier, Specifications specifications) { }
    public record ProductSummary(Long id, String name, Integer price, String currency, String imageUrl,
                                 Integer stock, List<String> tags, Supplier supplier) { }
    public record Supplier(Long id, String name, String country, Double internalRating) { }
    public record Specifications(String cpu, Integer memoryGb, Integer storageGb) { }
    public record CategoryResponse(Long id, String name, String description) { }
    public record ReviewResponse(String id, Integer rating, String comment, Reviewer reviewer) { }
    public record Reviewer(String name, String email) { }
    public record ReviewsResponse(Long productId, List<ReviewResponse> reviews) { }
    public record OrderItem(Long productId, String productName, Integer quantity, Integer unitPrice, Integer lineTotal) { }
    public record ShippingAddress(String city, String country, String internalZone) { }
    public record OrderResponse(String id, String status, Integer total, String currency, List<OrderItem> items,
                                ShippingAddress shippingAddress, String internalFulfilmentNote) { }
    public record CreateProductRequest(@NotBlank String name, @NotNull @Min(1) Integer price, @NotNull @Min(0) Integer stock) { }
    public record UpdateProductRequest(String name, @Min(1) Integer price, String description, @Min(0) Integer stock) { }
    public record OrderLineRequest(@NotNull Long productId, @NotNull @Min(1) Integer quantity) { }
    public record CreateOrderRequest(@NotEmpty List<@NotNull OrderLineRequest> items) { }
    public static class NotFoundException extends RuntimeException { public NotFoundException(String message) { super(message); } }
}
