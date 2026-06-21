package com.ecommerce.inventory.dto;

import com.ecommerce.inventory.domain.Product;
import java.time.Instant;

public record ProductResponse(
        Long id,
        String sku,
        String name,
        String description,
        int priceCents,
        int stockQty,
        Instant createdAt
) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getPriceCents(),
                product.getStockQty(),
                product.getCreatedAt()
        );
    }
}
