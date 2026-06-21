package com.ecommerce.dddlearning;

import static org.assertj.core.api.Assertions.assertThat;

import com.ecommerce.dddlearning.catalog.CatalogProduct;
import com.ecommerce.dddlearning.customer.CustomerId;
import com.ecommerce.dddlearning.integration.CatalogToSalesTranslator;
import com.ecommerce.dddlearning.order.Order;
import com.ecommerce.dddlearning.shipping.DeliveryRecipient;
import com.ecommerce.dddlearning.shipping.Shipment;
import com.ecommerce.dddlearning.shipping.ShippingAddress;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Explores <strong>Bounded Context</strong> from *Monolith to Microservices* (Chapter 1).
 * <p>
 * Run this test class first if aggregates are already familiar — it shows why we split
 * models instead of building one giant "Customer" / "Product" table for everything.
 */
class BoundedContextTest {

    @Test
    @DisplayName("Same word 'Product', different models: Catalog price can change; Order price is frozen")
    void productMeansDifferentThingsInCatalogAndSales() {
        var catalogProduct = new CatalogProduct(
                "Monolith to Microservices",
                "Sam Newman's guide to decomposition",
                "Books",
                3999
        );

        Order order = new Order(CustomerId.of(1L));
        CatalogToSalesTranslator.addToOrder(order, catalogProduct, 1);
        order.place();

        assertThat(order.lineItems().getFirst().unitPriceCents()).isEqualTo(3999);

        catalogProduct.updateListPrice(4999);
        assertThat(catalogProduct.getListPriceCents()).isEqualTo(4999);

        // Past orders keep the price the buyer saw at checkout
        assertThat(order.lineItems().getFirst().unitPriceCents()).isEqualTo(3999);
        assertThat(order.totalCents()).isEqualTo(3999);
    }

    @Test
    @DisplayName("Same person, different concepts: Sales has Customer; Shipping has Recipient")
    void customerAndRecipientAreDifferentModels() {
        Order order = new Order(CustomerId.of(42L));
        order.addProduct(101L, "Gift wrap", 1, 500);
        order.place();

        // Gift purchase: buyer is customer 42, but Alice receives the parcel
        var recipient = new DeliveryRecipient("Alice (gift recipient)", "+1-555-0100");
        var address = new ShippingAddress("1 Gift Lane", "Portland", "97201", "US");

        Shipment shipment = Shipment.schedule(
                99L,
                recipient,
                address,
                java.util.List.of(new Shipment.ShipmentLineRequest("Gift wrap", 1))
        );

        assertThat(order.getCustomerId()).isEqualTo(CustomerId.of(42L));
        assertThat(shipment.getRecipient().name()).isEqualTo("Alice (gift recipient)");
        assertThat(shipment.getOrderId()).isEqualTo(99L);
    }

    @Test
    @DisplayName("Contexts communicate by id and translated data, not shared entity references")
    void contextsReferenceEachOtherByIdOnly() {
        Order order = new Order(CustomerId.of(7L));
        order.addProduct(201L, "Building Microservices", 2, 4999);
        order.place();

        var recipient = new DeliveryRecipient("Bob Warehouse-contact", "+1-555-0199");
        var address = new ShippingAddress("42 Dock Road", "Seattle", "98101", "US");

        Shipment shipment = Shipment.schedule(
                1001L,
                recipient,
                address,
                order.lineItems().stream()
                        .map(item -> new Shipment.ShipmentLineRequest(item.productName(), item.quantity()))
                        .toList()
        );

        assertThat(shipment.getOrderId()).isEqualTo(1001L);
        assertThat(shipment.lines()).hasSize(1);
        assertThat(shipment.lines().getFirst().productName()).isEqualTo("Building Microservices");
        assertThat(shipment.lines().getFirst().quantity()).isEqualTo(2);
    }
}
