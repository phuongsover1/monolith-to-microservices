package com.ecommerce.order.dto;

import java.time.Instant;
import java.util.List;

import com.ecommerce.order.domain.Order;
import com.ecommerce.order.domain.OrderStatus;

public record OrderResponse(
        Long id,
        Long userId,
        String userEmail,
        OrderStatus status,
        int totalCents,
        Instant createdAt,
        List<OrderItemResponse> items) {
}
