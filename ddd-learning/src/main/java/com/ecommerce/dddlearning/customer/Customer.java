package com.ecommerce.dddlearning.customer;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A separate aggregate from Order. Other aggregates may only refer to this one
 * by {@link CustomerId}, never by holding a direct object reference.
 */
@Entity
@Table(name = "learning_customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    protected Customer() {
    }

    public Customer(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public CustomerId id() {
        return CustomerId.of(id);
    }
}
