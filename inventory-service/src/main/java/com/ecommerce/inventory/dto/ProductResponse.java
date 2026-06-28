package com.ecommerce.inventory.dto;

import java.time.Instant;

import com.ecommerce.inventory.domain.Product;

public record ProductResponse(
    Long id,
    String sku,
    String description,
    int priceCents,
    int stockQty,
    Instant createdAt
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(product.getId(),
        product.getSku(),
        product.getDescription(),
        product.getPriceCents(),
        product.getStockQty(),
        product.getCreatedAt());
    }
}
