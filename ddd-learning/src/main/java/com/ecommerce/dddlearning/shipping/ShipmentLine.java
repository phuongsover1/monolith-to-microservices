package com.ecommerce.dddlearning.shipping;

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
 * What the warehouse must pick and pack. Uses product <em>name</em> copied from Sales,
 * not a live link to Catalog — Shipping cares about physical fulfilment, not pricing.
 */
@Entity
@Table(name = "learning_shipment_lines")
class ShipmentLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(nullable = false)
    private int quantity;

    protected ShipmentLine() {
    }

    ShipmentLine(String productName, int quantity) {
        if (productName == null || productName.isBlank()) {
            throw new IllegalArgumentException("productName is required");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        this.productName = productName;
        this.quantity = quantity;
    }

    void attachTo(Shipment shipment) {
        this.shipment = shipment;
    }

    String getProductName() {
        return productName;
    }

    int getQuantity() {
        return quantity;
    }
}
