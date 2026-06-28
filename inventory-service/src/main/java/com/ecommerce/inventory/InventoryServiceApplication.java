package com.ecommerce.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the extracted Inventory microservice.
 *
 * <p>Runs on port 8081. The Strangler Fig proxy ({@code strangler-fig}) forwards
 * {@code /api/inventory/**} here during migration phases 2 and 3.
 *
 * <p>Chapter 3: this service shares the same database as the monolith.
 * Schema migrations remain owned by the monolith module.
 */
@SpringBootApplication
public class InventoryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventoryServiceApplication.class, args);
    }
}
