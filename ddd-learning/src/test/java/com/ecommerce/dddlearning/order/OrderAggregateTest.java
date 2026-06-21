package com.ecommerce.dddlearning.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ecommerce.dddlearning.customer.CustomerId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pure domain tests — no Spring context required.
 * Run these to explore how an aggregate root protects its boundary.
 */
class OrderAggregateTest {

    private Order order;

    @BeforeEach
    void setUp() {
        order = new Order(CustomerId.of(42L));
    }

    @Test
    @DisplayName("Order references Customer aggregate by id only, not by object reference")
    void orderStoresCustomerIdNotCustomerEntity() {
        assertThat(order.getCustomerId()).isEqualTo(CustomerId.of(42L));
        // There is no getCustomer() — crossing aggregate boundaries requires
        // fetching Customer separately when you actually need customer details.
    }

    @Test
    @DisplayName("Line items are added only through the aggregate root")
    void addProductThroughAggregateRoot() {
        order.addProduct(101L, "DDD Book", 1, 4500);

        assertThat(order.lineItems()).hasSize(1);
        assertThat(order.lineItems().getFirst().productName()).isEqualTo("DDD Book");
        assertThat(order.totalCents()).isEqualTo(4500);
    }

    @Test
    @DisplayName("Outside code receives read-only views, not mutable OrderLineItem entities")
    void lineItemsAreExposedAsImmutableViews() {
        order.addProduct(101L, "DDD Book", 2, 4500);

        var views = order.lineItems();
        assertThatThrownBy(() -> views.add(
                new OrderLineItemView(999L, "Hack", 1, 100, 100)
        )).isInstanceOf(UnsupportedOperationException.class);

        // OrderLineItem itself is package-private — this test file cannot even
        // reference `new OrderLineItem(...)` because it lives in another package.
    }

    @Test
    @DisplayName("Duplicate product ids merge quantities instead of creating orphan line items")
    void addingSameProductMergesQuantity() {
        order.addProduct(101L, "DDD Book", 1, 4500);
        order.addProduct(101L, "DDD Book", 2, 4500);

        assertThat(order.lineItems()).hasSize(1);
        assertThat(order.lineItems().getFirst().quantity()).isEqualTo(3);
        assertThat(order.totalCents()).isEqualTo(13500);
    }

    @Test
    @DisplayName("Cannot place an empty order — invariant enforced inside Order")
    void cannotPlaceEmptyOrder() {
        assertThatThrownBy(order::place)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no line items");
    }

    @Test
    @DisplayName("Placed orders reject structural changes — invariant enforced inside Order")
    void cannotModifyPlacedOrder() {
        order.addProduct(101L, "DDD Book", 1, 4500);
        order.place();

        assertThatThrownBy(() -> order.addProduct(102L, "Microservices Book", 1, 5000))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PLACED");

        assertThatThrownBy(() -> order.changeQuantity(101L, 5))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Quantity changes go through the aggregate root, not direct line-item mutation")
    void changeQuantityThroughAggregateRoot() {
        order.addProduct(101L, "DDD Book", 1, 4500);

        order.changeQuantity(101L, 4);

        assertThat(order.lineItems().getFirst().quantity()).isEqualTo(4);
        assertThat(order.totalCents()).isEqualTo(18000);
    }

    @Test
    @DisplayName("Invalid quantities are rejected before they reach a line item")
    void rejectsInvalidQuantity() {
        assertThatThrownBy(() -> order.addProduct(101L, "DDD Book", 0, 4500))
                .isInstanceOf(IllegalArgumentException.class);

        order.addProduct(101L, "DDD Book", 1, 4500);
        assertThatThrownBy(() -> order.changeQuantity(101L, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
