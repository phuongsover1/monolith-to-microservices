package com.ecommerce.strangler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Strangler Fig proxy.
 *
 * Architecture overview
 * =====================
 *
 *                        ┌──────────────────────────────────────┐
 *   Client               │         Strangler Fig Proxy           │
 *   (browser /     ───►  │                                       │
 *    mobile app /        │  StranglerFigFilter                   │
 *    other service)      │       │                               │
 *                        │       ▼                               │
 *                        │  StranglerRouter.route(path, method)  │
 *                        │       │                               │
 *                        │   [rules]                             │
 *                        │       │                               │
 *                        └───────┼───────────────────────────────┘
 *                                │
 *                  ┌─────────────┴──────────────┐
 *                  │                            │
 *                  ▼                            ▼
 *          ┌───────────────┐         ┌────────────────────┐
 *          │   Monolith    │         │ Inventory Service   │
 *          │ :8080         │         │ :8081               │
 *          │               │         │                     │
 *          │  /api/orders  │         │ /api/inventory/**   │
 *          │  /api/users   │         │                     │
 *          └───────────────┘         └────────────────────┘
 *
 * Migration lifecycle
 * ===================
 * Phase 1  No rules         → 100 % monolith
 * Phase 2  GET inventory    → inventory-service  |  rest → monolith
 * Phase 3  All inventory    → inventory-service  |  rest → monolith
 * Phase N  All routes done  → monolith receives zero traffic → decommission it
 *
 * See application.yml for per-phase configuration examples.
 */
@SpringBootApplication
public class StranglerFigApplication {

    public static void main(String[] args) {
        SpringApplication.run(StranglerFigApplication.class, args);
    }
}
