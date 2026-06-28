package com.ecommerce.strangler.routing;

import java.util.List;
import java.util.Map;

/**
 * The brain of the Strangler Fig proxy.
 *
 * How the Strangler Fig pattern works (Sam Newman, Ch. 3)
 * --------------------------------------------------------
 * The strangler fig tree grows around the host tree, wrapping it completely.
 * Over time the host tree dies and only the strangler fig remains.
 *
 * In software terms:
 *   1. Start with ALL traffic going to the monolith (no rules configured).
 *   2. Extract one capability into a new microservice.
 *   3. Add a RoutingRule that redirects that capability's traffic to the service.
 *   4. Repeat steps 2-3 for every capability you want to extract.
 *   5. Once all rules point to microservices, the monolith receives zero traffic
 *      and can be decommissioned — it has been "strangled".
 *
 * This class is intentionally framework-agnostic (plain Java) so it is easy
 * to unit-test.  Spring wiring is done in {@code StranglerFigConfig}.
 *
 * Rule evaluation
 * ---------------
 * Rules are evaluated in order; the FIRST matching rule wins.
 * If no rule matches, the request falls back to the monolith.
 */
public class StranglerRouter {

    private final List<RoutingRule> rules;
    private final Map<BackendType, String> backendUrls;

    public StranglerRouter(List<RoutingRule> rules, Map<BackendType, String> backendUrls) {
        this.rules = List.copyOf(rules);
        this.backendUrls = Map.copyOf(backendUrls);
    }

    /**
     * Decides which backend should handle the given request.
     *
     * @param path       the request URI (e.g. "/api/inventory/products")
     * @param httpMethod the HTTP verb (e.g. "GET")
     * @return a {@link RoutingDecision} describing the chosen backend and target URL
     */
    public RoutingDecision route(String path, String httpMethod) {
        return rules.stream()
                .filter(rule -> rule.matches(path, httpMethod))
                .findFirst()
                .map(rule -> new RoutingDecision(
                        rule.target(),
                        urlFor(rule.target()),
                        false
                ))
                .orElseGet(() -> new RoutingDecision(
                        BackendType.MONOLITH,
                        urlFor(BackendType.MONOLITH),
                        true  // no rule matched → default fallback to monolith
                ));
    }

    public List<RoutingRule> getRules() {
        return rules;
    }

    private String urlFor(BackendType backend) {
        String url = backendUrls.get(backend);
        if (url == null) {
            throw new IllegalStateException("No URL configured for backend: " + backend);
        }
        return url;
    }
}
