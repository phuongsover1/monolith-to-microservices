package com.ecommerce.strangler.proxy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ecommerce.strangler.routing.BackendType;
import com.ecommerce.strangler.routing.RoutingRule;
import com.ecommerce.strangler.routing.StranglerRouter;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;

/**
 * Integration tests for {@link StranglerFigFilter} — the HTTP proxy layer.
 *
 * These tests verify that:
 *   - The filter correctly reads the routing decision and forwards requests to
 *     the right backend URL.
 *   - Response status, headers, and body are faithfully proxied back to the
 *     caller.
 *   - Error responses from backends are propagated without modification.
 *
 * We use MockMvc (no real server socket) + a mocked RestTemplate (no real HTTP
 * calls to backends).  This makes the tests fast and deterministic.
 */
class StranglerFigFilterTest {

    private static final String MONOLITH_URL = "http://monolith-test:8080";
    private static final String INVENTORY_URL = "http://inventory-test:8081";

    private static final Map<BackendType, String> URLS = Map.of(
            BackendType.MONOLITH, MONOLITH_URL,
            BackendType.INVENTORY_SERVICE, INVENTORY_URL
    );

    // Mocked so we can verify which URL was called
    private RestTemplate restTemplate;
    private MockMvc mockMvc;

    private MockMvc buildMockMvc(StranglerRouter router) {
        StranglerFigFilter filter = new StranglerFigFilter(router, restTemplate);
        return MockMvcBuilders
                .standaloneSetup()          // no controllers — filter handles everything
                .addFilters(filter)
                .build();
    }

    @BeforeEach
    void setUp() {
        restTemplate = org.mockito.Mockito.mock(RestTemplate.class);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Phase 1: no rules → all calls forwarded to monolith
    // ──────────────────────────────────────────────────────────────────────────
    @Nested
    class Phase1_AllToMonolith {

        @BeforeEach
        void setUp() {
            StranglerRouter router = new StranglerRouter(List.of(), URLS);
            mockMvc = buildMockMvc(router);
        }

        @Test
        void inventoryGet_forwardedToMonolithUrl() throws Exception {
            stubBackendResponse("{\"id\":1,\"sku\":\"SKU-001\"}", HttpStatus.OK);

            mockMvc.perform(get("/api/inventory/products/1"))
                    .andExpect(status().isOk());

            URI calledUri = captureCalledUri();
            assertThat(calledUri.toString()).startsWith(MONOLITH_URL);
            assertThat(calledUri.getPath()).isEqualTo("/api/inventory/products/1");
        }

        @Test
        void ordersPost_forwardedToMonolithUrl() throws Exception {
            stubBackendResponse("{\"id\":99}", HttpStatus.CREATED);

            mockMvc.perform(post("/api/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"userId\":1}"));

            URI calledUri = captureCalledUri();
            assertThat(calledUri.toString()).startsWith(MONOLITH_URL);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Phase 2: inventory GETs → microservice, rest → monolith
    // ──────────────────────────────────────────────────────────────────────────
    @Nested
    class Phase2_InventoryReadsMigrated {

        @BeforeEach
        void setUp() {
            StranglerRouter router = new StranglerRouter(
                    List.of(new RoutingRule("/api/inventory/**", "GET", BackendType.INVENTORY_SERVICE)),
                    URLS
            );
            mockMvc = buildMockMvc(router);
        }

        @Test
        void inventoryGet_forwardedToInventoryService() throws Exception {
            stubBackendResponse("{\"id\":1,\"sku\":\"SKU-001\"}", HttpStatus.OK);

            mockMvc.perform(get("/api/inventory/products/1"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("{\"id\":1,\"sku\":\"SKU-001\"}"));

            URI calledUri = captureCalledUri();
            // ← KEY ASSERTION: request reached the microservice, NOT the monolith
            assertThat(calledUri.toString()).startsWith(INVENTORY_URL);
            assertThat(calledUri.toString()).doesNotContain(MONOLITH_URL);
        }

        @Test
        void inventoryPost_stillForwardedToMonolith() throws Exception {
            stubBackendResponse("{\"id\":2}", HttpStatus.CREATED);

            mockMvc.perform(post("/api/inventory/products")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"sku\":\"NEW-001\",\"name\":\"Widget\",\"priceCents\":999,\"stockQty\":50}"));

            URI calledUri = captureCalledUri();
            // ← Writes are NOT yet migrated; monolith handles them
            assertThat(calledUri.toString()).startsWith(MONOLITH_URL);
        }

        @Test
        void ordersGet_forwardedToMonolith() throws Exception {
            stubBackendResponse("[]", HttpStatus.OK);

            mockMvc.perform(get("/api/orders?userId=1"));

            URI calledUri = captureCalledUri();
            assertThat(calledUri.toString()).startsWith(MONOLITH_URL);
        }

        @Test
        void queryStringIsPreservedInForwardedUrl() throws Exception {
            stubBackendResponse("[]", HttpStatus.OK);

            mockMvc.perform(get("/api/inventory/products?page=0&size=10"));

            URI calledUri = captureCalledUri();
            assertThat(calledUri.toString()).startsWith(INVENTORY_URL);
            assertThat(calledUri.getQuery()).isEqualTo("page=0&size=10");
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Phase 3: all inventory traffic → microservice
    // ──────────────────────────────────────────────────────────────────────────
    @Nested
    class Phase3_InventoryFullyMigrated {

        @BeforeEach
        void setUp() {
            StranglerRouter router = new StranglerRouter(
                    List.of(new RoutingRule("/api/inventory/**", "*", BackendType.INVENTORY_SERVICE)),
                    URLS
            );
            mockMvc = buildMockMvc(router);
        }

        @Test
        void inventoryPost_nowForwardedToInventoryService() throws Exception {
            stubBackendResponse("{\"id\":2,\"sku\":\"NEW-001\"}", HttpStatus.CREATED);

            mockMvc.perform(post("/api/inventory/products")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"sku\":\"NEW-001\"}"));

            URI calledUri = captureCalledUri();
            // ← Writes are NOW migrated
            assertThat(calledUri.toString()).startsWith(INVENTORY_URL);
        }

        @Test
        void ordersPost_stillForwardedToMonolith() throws Exception {
            stubBackendResponse("{\"id\":99}", HttpStatus.CREATED);

            mockMvc.perform(post("/api/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"userId\":1}"));

            URI calledUri = captureCalledUri();
            // Orders not migrated yet
            assertThat(calledUri.toString()).startsWith(MONOLITH_URL);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Error handling: backend errors are forwarded faithfully
    // ──────────────────────────────────────────────────────────────────────────
    @Nested
    class ErrorHandling {

        @BeforeEach
        void setUp() {
            StranglerRouter router = new StranglerRouter(
                    List.of(new RoutingRule("/api/inventory/**", "GET", BackendType.INVENTORY_SERVICE)),
                    URLS
            );
            mockMvc = buildMockMvc(router);
        }

        @Test
        void backendReturns404_proxiedBackToCaller() throws Exception {
            stubBackendResponse("{\"message\":\"Product not found\"}", HttpStatus.NOT_FOUND);

            mockMvc.perform(get("/api/inventory/products/9999"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void backendReturns500_proxiedBackToCaller() throws Exception {
            stubBackendResponse("{\"error\":\"Internal Server Error\"}", HttpStatus.INTERNAL_SERVER_ERROR);

            mockMvc.perform(get("/api/inventory/products"))
                    .andExpect(status().isInternalServerError());
        }
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void stubBackendResponse(String body, HttpStatus status) {
        when(restTemplate.exchange(
                any(URI.class),
                any(HttpMethod.class),
                any(),
                eq(byte[].class)
        )).thenReturn(ResponseEntity.status(status).body(body.getBytes()));
    }

    @SuppressWarnings("unchecked")
    private URI captureCalledUri() {
        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        verify(restTemplate).exchange(
                uriCaptor.capture(),
                any(HttpMethod.class),
                any(),
                eq(byte[].class)
        );
        return uriCaptor.getValue();
    }
}
