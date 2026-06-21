package com.ecommerce.dddlearning.catalog;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Aggregate root for the Catalog bounded context.
 * <p>
 * The list price can change at any time. Sales captures a snapshot when an order
 * is built — past orders are never retroactively repriced.
 */
@Entity
@Table(name = "learning_catalog_products")
public class CatalogProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String description;

    private String category;

    private int listPriceCents;

    protected CatalogProduct() {
    }

    public CatalogProduct(String name, String description, String category, int listPriceCents) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        if (listPriceCents < 0) {
            throw new IllegalArgumentException("listPriceCents cannot be negative");
        }
        this.name = name;
        this.description = description;
        this.category = category;
        this.listPriceCents = listPriceCents;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public int getListPriceCents() {
        return listPriceCents;
    }

    public void updateListPrice(int newListPriceCents) {
        if (newListPriceCents < 0) {
            throw new IllegalArgumentException("listPriceCents cannot be negative");
        }
        this.listPriceCents = newListPriceCents;
    }
}
