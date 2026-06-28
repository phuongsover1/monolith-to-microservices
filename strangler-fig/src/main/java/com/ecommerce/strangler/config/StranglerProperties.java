package com.ecommerce.strangler.config;

import com.ecommerce.strangler.routing.BackendType;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalised configuration for the Strangler Fig proxy.
 *
 * The key idea: you control the migration entirely through configuration.
 * No code changes are needed to redirect traffic — just add/remove a route
 * entry in application.yml (or an environment variable / feature-flag system).
 *
 * Example application.yml (see resources/application.yml for full examples):
 * <pre>
 * strangler:
 *   monolith-url: http://monolith:8080
 *   inventory-service-url: http://inventory-service:8081
 *   routes:
 *     - path-pattern: /api/inventory/**
 *       http-method: GET
 *       target: INVENTORY_SERVICE
 * </pre>
 */
@ConfigurationProperties(prefix = "strangler")
public record StranglerProperties(

        /** Base URL of the legacy monolith — the default fallback backend. */
        String monolithUrl,

        /** Base URL of the extracted Inventory microservice. */
        String inventoryServiceUrl,

        /**
         * Ordered list of routing rules.
         * The first matching rule wins; unmatched requests fall back to the monolith.
         */
        List<RouteConfig> routes

) {

    public StranglerProperties {
        routes = (routes == null) ? List.of() : List.copyOf(routes);
    }

    /** One routing rule as read from the YAML configuration. */
    public record RouteConfig(
            String pathPattern,
            String httpMethod,
            BackendType target
    ) {}
}
