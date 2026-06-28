package com.ecommerce.strangler.routing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link StranglerRouter} — one test class per migration phase.
 *
 * Each nested class documents a concrete step in the Strangler Fig migration
 * of the Inventory capability out of the monolith.  Read them in order:
 *   Phase1 → Phase2 → Phase3 → Phase4
 *
 * The tests use no Spring context: the router is pure Java and fully testable
 * without starting a server.
 */
class StranglerRouterTest {

    private static final Map<BackendType, String> BACKEND_URLS = Map.of(
            BackendType.MONOLITH, "http://monolith:8080",
            BackendType.INVENTORY_SERVICE, "http://inventory:8081"
    );

    // ──────────────────────────────────────────────────────────────────────────
    // PHASE 1 — Starting state: zero rules, all traffic goes to the monolith.
    //
    // The proxy is deployed but transparent: every request is forwarded to the
    // monolith exactly as before.  No client changes are needed.  We now have
    // the "intercept" hook in place — the first step of the Strangler Fig.
    // ──────────────────────────────────────────────────────────────────────────
    @Nested
    class Phase1_NoRules_AllTrafficToMonolith {

        private final StranglerRouter router = new StranglerRouter(List.of(), BACKEND_URLS);

        @Test
        void inventoryGet_goesToMonolith() {
            assertBackend(router.route("/api/inventory/products", "GET"), BackendType.MONOLITH);
        }

        @Test
        void inventoryPost_goesToMonolith() {
            assertBackend(router.route("/api/inventory/products", "POST"), BackendType.MONOLITH);
        }

        @Test
        void ordersPost_goesToMonolith() {
            assertBackend(router.route("/api/orders", "POST"), BackendType.MONOLITH);
        }

        @Test
        void usersGet_goesToMonolith() {
            assertBackend(router.route("/api/users/1", "GET"), BackendType.MONOLITH);
        }

        @Test
        void allDecisionsAreDefaultFallbacks() {
            assertThat(router.route("/api/inventory/products", "GET").isDefaultFallback()).isTrue();
            assertThat(router.route("/api/orders", "POST").isDefaultFallback()).isTrue();
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // PHASE 2 — Inventory READ requests are migrated first.
    //
    // Sam Newman recommends migrating reads before writes because reads are
    // idempotent and have no side effects.  If the new service produces wrong
    // results, the worst outcome is a stale read — no data is corrupted.
    //
    // At this point:
    //   GET  /api/inventory/** → Inventory Microservice  ← NEW
    //   POST /api/inventory/** → Monolith               (writes not yet trusted)
    //   *    /api/orders/**    → Monolith
    // ──────────────────────────────────────────────────────────────────────────
    @Nested
    class Phase2_InventoryReadsMigrated {

        private final StranglerRouter router = new StranglerRouter(
                List.of(
                        new RoutingRule("/api/inventory/**", "GET", BackendType.INVENTORY_SERVICE)
                ),
                BACKEND_URLS
        );

        @Test
        void inventoryGet_goesToMicroservice() {
            RoutingDecision decision = router.route("/api/inventory/products", "GET");
            assertBackend(decision, BackendType.INVENTORY_SERVICE);
            assertThat(decision.isDefaultFallback()).isFalse();
        }

        @Test
        void inventoryGetById_goesToMicroservice() {
            assertBackend(
                    router.route("/api/inventory/products/42", "GET"),
                    BackendType.INVENTORY_SERVICE
            );
        }

        @Test
        void inventoryPost_stillGoesToMonolith() {
            // Write path not migrated yet — monolith owns it
            assertBackend(router.route("/api/inventory/products", "POST"), BackendType.MONOLITH);
        }

        @Test
        void inventoryRestock_stillGoesToMonolith() {
            assertBackend(
                    router.route("/api/inventory/products/1/restock", "POST"),
                    BackendType.MONOLITH
            );
        }

        @Test
        void orders_stillGoesToMonolith() {
            assertBackend(router.route("/api/orders", "POST"), BackendType.MONOLITH);
        }

        @Test
        void users_stillGoesToMonolith() {
            assertBackend(router.route("/api/users/1", "GET"), BackendType.MONOLITH);
        }

        @Test
        void targetUrlContainsMicroserviceHost() {
            RoutingDecision decision = router.route("/api/inventory/products", "GET");
            String url = decision.buildTargetUrl("/api/inventory/products", null);
            assertThat(url).startsWith("http://inventory:8081");
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // PHASE 3 — Inventory fully migrated (reads AND writes).
    //
    // After validating reads in production, we are now confident enough to
    // migrate writes as well.  All inventory traffic goes to the microservice.
    // The monolith still handles orders and users.
    // ──────────────────────────────────────────────────────────────────────────
    @Nested
    class Phase3_InventoryFullyMigrated {

        private final StranglerRouter router = new StranglerRouter(
                List.of(
                        new RoutingRule("/api/inventory/**", "*", BackendType.INVENTORY_SERVICE)
                ),
                BACKEND_URLS
        );

        @Test
        void inventoryGet_goesToMicroservice() {
            assertBackend(router.route("/api/inventory/products", "GET"), BackendType.INVENTORY_SERVICE);
        }

        @Test
        void inventoryPost_goesToMicroservice() {
            assertBackend(router.route("/api/inventory/products", "POST"), BackendType.INVENTORY_SERVICE);
        }

        @Test
        void inventoryPut_goesToMicroservice() {
            assertBackend(router.route("/api/inventory/products/1", "PUT"), BackendType.INVENTORY_SERVICE);
        }

        @Test
        void inventoryDelete_goesToMicroservice() {
            assertBackend(router.route("/api/inventory/products/1", "DELETE"), BackendType.INVENTORY_SERVICE);
        }

        @Test
        void inventoryRestock_goesToMicroservice() {
            assertBackend(
                    router.route("/api/inventory/products/1/restock", "POST"),
                    BackendType.INVENTORY_SERVICE
            );
        }

        @Test
        void orders_stillGoesToMonolith() {
            assertBackend(router.route("/api/orders", "POST"), BackendType.MONOLITH);
        }

        @Test
        void users_stillGoesToMonolith() {
            assertBackend(router.route("/api/users/1", "GET"), BackendType.MONOLITH);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // PHASE 4 — All capabilities extracted; monolith receives zero traffic.
    //
    // At this point the monolith is "strangled": it is still running (safe
    // fallback), but no request actually reaches it.  It can now be
    // decommissioned.  This is the end goal of the pattern.
    //
    // (This demo only shows the inventory service.  In a real project you would
    //  add ORDER_SERVICE, USER_SERVICE, etc.)
    // ──────────────────────────────────────────────────────────────────────────
    @Nested
    class Phase4_MonolithStranguled {

        private final StranglerRouter router = new StranglerRouter(
                List.of(
                        new RoutingRule("/api/inventory/**", "*", BackendType.INVENTORY_SERVICE)
                        // In a complete migration you would add:
                        // new RoutingRule("/api/orders/**",  "*", BackendType.ORDER_SERVICE),
                        // new RoutingRule("/api/users/**",   "*", BackendType.USER_SERVICE),
                ),
                BACKEND_URLS
        );

        @Test
        void inventoryTraffic_doesNotReachMonolith() {
            assertThat(router.route("/api/inventory/products", "GET").backend())
                    .isNotEqualTo(BackendType.MONOLITH);

            assertThat(router.route("/api/inventory/products", "POST").backend())
                    .isNotEqualTo(BackendType.MONOLITH);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Rule ordering — first matching rule wins
    // ──────────────────────────────────────────────────────────────────────────
    @Nested
    class RuleOrdering {

        @Test
        void firstMatchingRuleWins() {
            // Two rules for the same path: the first one should win
            StranglerRouter router = new StranglerRouter(
                    List.of(
                            new RoutingRule("/api/inventory/**", "GET", BackendType.INVENTORY_SERVICE),
                            new RoutingRule("/api/inventory/**", "GET", BackendType.MONOLITH) // shadowed
                    ),
                    BACKEND_URLS
            );

            assertBackend(router.route("/api/inventory/products", "GET"), BackendType.INVENTORY_SERVICE);
        }
    }

    // ─── helper ──────────────────────────────────────────────────────────────

    private void assertBackend(RoutingDecision decision, BackendType expected) {
        assertThat(decision.backend())
                .as("Expected backend to be %s but was %s", expected, decision.backend())
                .isEqualTo(expected);
    }
}
