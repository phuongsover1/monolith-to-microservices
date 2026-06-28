package com.ecommerce.strangler.routing;

/**
 * The outcome of a routing evaluation: which backend to call and its base URL.
 *
 * {@code isDefaultFallback} is {@code true} when no explicit rule matched and
 * the request falls back to the monolith — useful for logging and metrics.
 */
public record RoutingDecision(
        BackendType backend,
        String baseUrl,
        boolean isDefaultFallback
) {

    /**
     * Builds the full target URL by appending the original request path (and
     * optional query string) to the backend's base URL.
     */
    public String buildTargetUrl(String path, String queryString) {
        String target = baseUrl + path;
        return (queryString != null && !queryString.isEmpty())
                ? target + "?" + queryString
                : target;
    }
}
