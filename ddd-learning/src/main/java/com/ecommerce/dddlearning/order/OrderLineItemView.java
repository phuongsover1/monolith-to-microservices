package com.ecommerce.dddlearning.order;

/**
 * Read-only snapshot exposed by the aggregate root.
 * Callers see data, not mutable entities.
 */
public record OrderLineItemView(
        Long productId,
        String productName,
        int quantity,
        int unitPriceCents,
        int lineTotalCents
) {
    static OrderLineItemView from(OrderLineItem item) {
        return new OrderLineItemView(
                item.getProductId(),
                item.getProductName(),
                item.getQuantity(),
                item.getUnitPriceCents(),
                item.lineTotalCents()
        );
    }
}
