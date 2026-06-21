package com.ecommerce.dddlearning.shipping;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;

/**
 * Who receives the parcel — Shipping's term for a person at a delivery location.
 * <p>
 * This is deliberately <strong>not</strong> called "Customer". A gift order might have
 * a buyer (Customer in Sales) and a different recipient here.
 */
@Embeddable
public record DeliveryRecipient(
        @Column(name = "recipient_name", nullable = false) String name,
        @Column(name = "recipient_phone", nullable = false) String contactPhone
) {

    public DeliveryRecipient {
        Objects.requireNonNull(name, "recipient name is required");
        if (name.isBlank()) {
            throw new IllegalArgumentException("recipient name cannot be blank");
        }
        Objects.requireNonNull(contactPhone, "contactPhone is required");
        if (contactPhone.isBlank()) {
            throw new IllegalArgumentException("contactPhone cannot be blank");
        }
    }
}
