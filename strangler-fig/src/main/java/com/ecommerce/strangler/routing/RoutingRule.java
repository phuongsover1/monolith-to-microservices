package com.ecommerce.strangler.routing;

/**
 * A single routing rule that maps an (HTTP-method, URL-path-pattern) pair
 * to a specific backend.
 *
 * Path-pattern syntax
 * -------------------
 *   "/api/inventory/**"  →  matches any path that starts with /api/inventory/
 *   "/api/inventory/products"  →  exact match only
 *
 * HTTP-method
 * -----------
 *   "GET", "POST", "PUT", …  →  matches that specific method
 *   "*"                      →  matches ALL methods
 *
 * Migration example (Chapter 3, Sam Newman)
 * -----------------------------------------
 * Phase 2 – reads migrated first (safer, no write risk):
 *   new RoutingRule("/api/inventory/**", "GET", BackendType.INVENTORY_SERVICE)
 *
 * Phase 3 – writes also migrated (full extraction):
 *   new RoutingRule("/api/inventory/**", "*", BackendType.INVENTORY_SERVICE)
 */
public record RoutingRule(
        String pathPattern,
        String httpMethod,
        BackendType target
) {

    public boolean matches(String path, String method) {
        return matchesPath(path) && matchesMethod(method);
    }

    private boolean matchesPath(String path) {
        if (pathPattern.endsWith("/**")) {
            String prefix = pathPattern.substring(0, pathPattern.length() - 3);
            // Require a "/" boundary so "/api/inventory/**" does NOT match
            // "/api/inventory-archive/..." (which shares the same prefix string).
            return path.equals(prefix) || path.startsWith(prefix + "/");
        }
        return pathPattern.equals(path);
    }

    private boolean matchesMethod(String method) {
        return "*".equals(httpMethod) || httpMethod.equalsIgnoreCase(method);
    }
}
