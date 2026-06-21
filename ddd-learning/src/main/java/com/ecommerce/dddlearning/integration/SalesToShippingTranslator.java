package com.ecommerce.dddlearning.integration;

import com.ecommerce.dddlearning.order.Order;
import com.ecommerce.dddlearning.shipping.DeliveryRecipient;
import com.ecommerce.dddlearning.shipping.Shipment;
import com.ecommerce.dddlearning.shipping.ShippingAddress;
import java.util.Objects;

/**
 * Anti-corruption layer at the Sales → Shipping boundary.
 * <p>
 * Shipping never sees {@link Order} entities. When an order is placed, Sales
 * publishes the facts Shipping needs — translated into Shipping's ubiquitous language.
 */
public final class SalesToShippingTranslator {

    private SalesToShippingTranslator() {
    }

    public static Shipment scheduleShipment(
            Order placedOrder,
            DeliveryRecipient recipient,
            ShippingAddress address
    ) {
        Objects.requireNonNull(placedOrder, "placedOrder is required");
        if (placedOrder.getId() == null) {
            throw new IllegalArgumentException("placedOrder must be persisted before scheduling shipment");
        }

        var lines = placedOrder.lineItems().stream()
                .map(item -> new Shipment.ShipmentLineRequest(item.productName(), item.quantity()))
                .toList();

        return Shipment.schedule(placedOrder.getId(), recipient, address, lines);
    }
}
