package com.ecommerce.order.dto;

import com.ecommerce.inventory.dto.ProductResponse;
import com.ecommerce.order.domain.OrderItem;

public record OrderItemResponse(
        Long id,
        Long productId,
        String productSku,
        String productName,
        int quantity,
        int unitPriceCents) {

    public static OrderItemResponse from(OrderItem item, ProductResponse product) {
        return new OrderItemResponse(
                item.getId(),
                product.id(),
                product.sku(),
                product.name(),
                item.getQuantity(),
                item.getUnitPriceCents());
    }
}
