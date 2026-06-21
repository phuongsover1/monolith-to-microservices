package com.ecommerce.order.dto;

import com.ecommerce.order.domain.OrderItem;

public record OrderItemResponse(
        Long id,
        Long productId,
        String productSku,
        String productName,
        int quantity,
        int unitPriceCents
) {

    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getProduct().getId(),
                item.getProduct().getSku(),
                item.getProduct().getName(),
                item.getQuantity(),
                item.getUnitPriceCents()
        );
    }
}
