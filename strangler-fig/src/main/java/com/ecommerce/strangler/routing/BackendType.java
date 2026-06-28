package com.ecommerce.strangler.routing;

/**
 * Represents the possible backends that can handle an HTTP request.
 *
 * In the Strangler Fig pattern the proxy can forward traffic to:
 *   - MONOLITH  : the existing legacy system (always available as a fallback)
 *   - INVENTORY_SERVICE : the new microservice extracted from the monolith
 *
 * As more capabilities are migrated you would add more enum values here
 * (ORDER_SERVICE, USER_SERVICE, …).  Once a service handles 100 % of its
 * routes the corresponding monolith code can be deleted.
 */
public enum BackendType {

    MONOLITH("Monolith (legacy)"),
    INVENTORY_SERVICE("Inventory Microservice");

    private final String displayName;

    BackendType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
