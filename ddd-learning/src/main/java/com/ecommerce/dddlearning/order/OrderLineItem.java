package com.ecommerce.dddlearning.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Local entity inside the Order aggregate boundary.
 * <p>
 * Package-private on purpose: callers outside this package cannot construct or
 * mutate line items directly. Every change must go through {@link Order}.
 */
@Entity
@Table(name = "learning_order_line_items")
class OrderLineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_price_cents", nullable = false)
    private int unitPriceCents;

    protected OrderLineItem() {
    }

    OrderLineItem(Long productId, String productName, int quantity, int unitPriceCents) {
        if (productId == null || productId <= 0) {
            throw new IllegalArgumentException("productId must be positive");
        }
        if (productName == null || productName.isBlank()) {
            throw new IllegalArgumentException("productName is required");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        if (unitPriceCents < 0) {
            throw new IllegalArgumentException("unitPriceCents cannot be negative");
        }
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPriceCents = unitPriceCents;
    }

    void attachTo(Order order) {
        this.order = order;
    }

    void changeQuantity(int newQuantity) {
        if (newQuantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        this.quantity = newQuantity;
    }

    Long getProductId() {
        return productId;
    }

    String getProductName() {
        return productName;
    }

    int getQuantity() {
        return quantity;
    }

    int getUnitPriceCents() {
        return unitPriceCents;
    }

    int lineTotalCents() {
        return quantity * unitPriceCents;
    }
}
