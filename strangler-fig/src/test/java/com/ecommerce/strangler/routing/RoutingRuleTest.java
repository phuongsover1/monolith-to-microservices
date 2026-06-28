package com.ecommerce.strangler.routing;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link RoutingRule} path-pattern and HTTP-method matching.
 *
 * These tests verify that the rule matching logic works correctly before we
 * plug it into the full router.  Think of this as testing the "if" condition
 * inside the Strangler Fig decision engine.
 */
class RoutingRuleTest {

    // ─── Path-pattern matching ────────────────────────────────────────────────

    @Nested
    class WildcardPathPattern {

        private final RoutingRule rule =
                new RoutingRule("/api/inventory/**", "*", BackendType.INVENTORY_SERVICE);

        @Test
        void matchesDirectChildPath() {
            assertThat(rule.matches("/api/inventory/products", "GET")).isTrue();
        }

        @Test
        void matchesNestedChildPath() {
            assertThat(rule.matches("/api/inventory/products/42/restock", "POST")).isTrue();
        }

        @Test
        void doesNotMatchDifferentRootPath() {
            assertThat(rule.matches("/api/orders/1", "GET")).isFalse();
        }

        @Test
        void doesNotMatchPartialPrefix() {
            assertThat(rule.matches("/api/inventory-archive/products", "GET")).isFalse();
        }
    }

    @Nested
    class ExactPathPattern {

        private final RoutingRule rule =
                new RoutingRule("/api/inventory/products", "GET", BackendType.INVENTORY_SERVICE);

        @Test
        void matchesExactPath() {
            assertThat(rule.matches("/api/inventory/products", "GET")).isTrue();
        }

        @Test
        void doesNotMatchChildOfExactPath() {
            assertThat(rule.matches("/api/inventory/products/42", "GET")).isFalse();
        }
    }

    // ─── HTTP-method matching ─────────────────────────────────────────────────

    @Nested
    class HttpMethodMatching {

        @Test
        void wildcardMethodMatchesAnyVerb() {
            RoutingRule rule = new RoutingRule("/api/inventory/**", "*", BackendType.INVENTORY_SERVICE);

            assertThat(rule.matches("/api/inventory/products", "GET")).isTrue();
            assertThat(rule.matches("/api/inventory/products", "POST")).isTrue();
            assertThat(rule.matches("/api/inventory/products", "PUT")).isTrue();
            assertThat(rule.matches("/api/inventory/products", "DELETE")).isTrue();
        }

        @Test
        void specificMethodOnlyMatchesThatVerb() {
            RoutingRule rule = new RoutingRule("/api/inventory/**", "GET", BackendType.INVENTORY_SERVICE);

            assertThat(rule.matches("/api/inventory/products", "GET")).isTrue();
            assertThat(rule.matches("/api/inventory/products", "POST")).isFalse();
            assertThat(rule.matches("/api/inventory/products", "DELETE")).isFalse();
        }

        @Test
        void methodMatchingIsCaseInsensitive() {
            RoutingRule rule = new RoutingRule("/api/inventory/**", "GET", BackendType.INVENTORY_SERVICE);

            assertThat(rule.matches("/api/inventory/products", "get")).isTrue();
            assertThat(rule.matches("/api/inventory/products", "Get")).isTrue();
        }
    }
}
