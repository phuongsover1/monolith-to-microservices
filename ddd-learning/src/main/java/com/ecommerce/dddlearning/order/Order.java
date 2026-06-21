package com.ecommerce.dddlearning.order;

import com.ecommerce.dddlearning.customer.CustomerId;
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
 * Aggregate root for the Order bounded context.
 * <p>
 * Invariants enforced here:
 * <ul>
 *   <li>Every order belongs to exactly one customer, referenced by id only.</li>
 *   <li>Line items cannot exist outside this aggregate.</li>
 *   <li>An order must contain at least one line item before it can be placed.</li>
 *   <li>Only draft orders can be modified or placed.</li>
 * </ul>
 */
@Entity
@Table(name = "learning_orders")
public class Order {

    private static final int MAX_LINE_ITEMS = 50;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    private CustomerId customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderLineItem> lineItems = new ArrayList<>();

    protected Order() {
    }

    public Order(CustomerId customerId) {
        this.customerId = Objects.requireNonNull(customerId, "customerId is required");
        this.status = OrderStatus.DRAFT;
    }

    public Long getId() {
        return id;
    }

    public CustomerId getCustomerId() {
        return customerId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    /**
     * Exposes an immutable view — not the live {@link OrderLineItem} entities.
     */
    public List<OrderLineItemView> lineItems() {
        return Collections.unmodifiableList(
                lineItems.stream().map(OrderLineItemView::from).toList()
        );
    }

    public int totalCents() {
        return lineItems.stream().mapToInt(OrderLineItem::lineTotalCents).sum();
    }

    public void addProduct(Long productId, String productName, int quantity, int unitPriceCents) {
        assertDraft("add products to");
        if (lineItems.size() >= MAX_LINE_ITEMS) {
            throw new IllegalStateException("An order cannot contain more than " + MAX_LINE_ITEMS + " line items");
        }

        OrderLineItem existing = findLineItem(productId);
        if (existing != null) {
            existing.changeQuantity(existing.getQuantity() + quantity);
            return;
        }

        OrderLineItem lineItem = new OrderLineItem(productId, productName, quantity, unitPriceCents);
        lineItem.attachTo(this);
        lineItems.add(lineItem);
    }

    public void changeQuantity(Long productId, int newQuantity) {
        assertDraft("change quantities on");
        OrderLineItem lineItem = requireLineItem(productId);
        lineItem.changeQuantity(newQuantity);
    }

    public void removeProduct(Long productId) {
        assertDraft("remove products from");
        lineItems.removeIf(item -> item.getProductId().equals(productId));
    }

    public void place() {
        assertDraft("place");
        if (lineItems.isEmpty()) {
            throw new IllegalStateException("Cannot place an order with no line items");
        }
        status = OrderStatus.PLACED;
    }

    public void cancel() {
        if (status != OrderStatus.PLACED) {
            throw new IllegalStateException("Only placed orders can be cancelled");
        }
        status = OrderStatus.CANCELLED;
    }

    private void assertDraft(String action) {
        if (status != OrderStatus.DRAFT) {
            throw new IllegalStateException("Cannot " + action + " a " + status + " order");
        }
    }

    private OrderLineItem findLineItem(Long productId) {
        return lineItems.stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst()
                .orElse(null);
    }

    private OrderLineItem requireLineItem(Long productId) {
        OrderLineItem lineItem = findLineItem(productId);
        if (lineItem == null) {
            throw new IllegalArgumentException("Product " + productId + " is not on this order");
        }
        return lineItem;
    }
}
