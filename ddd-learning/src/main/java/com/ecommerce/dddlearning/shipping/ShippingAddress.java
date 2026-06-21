package com.ecommerce.dddlearning.shipping;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;

@Embeddable
public record ShippingAddress(
        @Column(name = "street", nullable = false) String street,
        @Column(name = "city", nullable = false) String city,
        @Column(name = "postal_code", nullable = false) String postalCode,
        @Column(name = "country", nullable = false) String country
) {

    public ShippingAddress {
        Objects.requireNonNull(street, "street is required");
        Objects.requireNonNull(city, "city is required");
        Objects.requireNonNull(postalCode, "postalCode is required");
        Objects.requireNonNull(country, "country is required");
        if (street.isBlank() || city.isBlank() || postalCode.isBlank() || country.isBlank()) {
            throw new IllegalArgumentException("address fields cannot be blank");
        }
    }
}
