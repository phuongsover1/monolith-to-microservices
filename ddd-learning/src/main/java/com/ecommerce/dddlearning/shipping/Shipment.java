package com.ecommerce.dddlearning.shipping;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Aggregate root for the Shipping bounded context.
 * <p>
 * References a placed order by {@code orderId} only — never by holding an
 * {@code Order} entity from the Sales context.
 */
@Entity
@Table(name = "learning_shipments")
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Embedded
    private DeliveryRecipient recipient;

    @Embedded
    private ShippingAddress address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShipmentStatus status;

    @OneToMany(mappedBy = "shipment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ShipmentLine> lines = new ArrayList<>();

    protected Shipment() {
    }

    private Shipment(Long orderId, DeliveryRecipient recipient, ShippingAddress address) {
        this.orderId = Objects.requireNonNull(orderId, "orderId is required");
        if (orderId <= 0) {
            throw new IllegalArgumentException("orderId must be positive");
        }
        this.recipient = Objects.requireNonNull(recipient, "recipient is required");
        this.address = Objects.requireNonNull(address, "address is required");
        this.status = ShipmentStatus.SCHEDULED;
    }

    /**
     * Factory used at the context boundary after an order is placed in Sales.
     */
    public static Shipment schedule(
            Long orderId,
            DeliveryRecipient recipient,
            ShippingAddress address,
            List<ShipmentLineRequest> lines
    ) {
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("Cannot schedule a shipment with no lines");
        }

        Shipment shipment = new Shipment(orderId, recipient, address);
        for (ShipmentLineRequest line : lines) {
            ShipmentLine shipmentLine = new ShipmentLine(line.productName(), line.quantity());
            shipmentLine.attachTo(shipment);
            shipment.lines.add(shipmentLine);
        }
        return shipment;
    }

    public Long getId() {
        return id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public DeliveryRecipient getRecipient() {
        return recipient;
    }

    public ShippingAddress getAddress() {
        return address;
    }

    public ShipmentStatus getStatus() {
        return status;
    }

    public List<ShipmentLineView> lines() {
        return Collections.unmodifiableList(
                lines.stream().map(line -> new ShipmentLineView(line.getProductName(), line.getQuantity())).toList()
        );
    }

    public void dispatch() {
        if (status != ShipmentStatus.SCHEDULED) {
            throw new IllegalStateException("Only scheduled shipments can be dispatched");
        }
        status = ShipmentStatus.DISPATCHED;
    }

    public record ShipmentLineRequest(String productName, int quantity) {
    }

    public record ShipmentLineView(String productName, int quantity) {
    }
}
