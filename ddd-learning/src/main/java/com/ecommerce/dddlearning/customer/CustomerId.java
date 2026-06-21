package com.ecommerce.dddlearning.customer;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;

/**
 * Identity of the Customer aggregate, stored by reference inside other aggregates.
 * This is deliberately NOT a JPA {@code @ManyToOne Customer} — that would blur
 * aggregate boundaries and let Hibernate load a foreign aggregate graph.
 */
@Embeddable
public record CustomerId(@Column(name = "customer_id", nullable = false) Long value) {

    public CustomerId {
        Objects.requireNonNull(value, "customerId is required");
        if (value <= 0) {
            throw new IllegalArgumentException("customerId must be positive");
        }
    }

    public static CustomerId of(long value) {
        return new CustomerId(value);
    }
}
