# DDD Learning Sandbox

This module is **isolated** from the main ecommerce monolith. Use it to experiment with Domain-Driven Design concepts from *Monolith to Microservices* (Chapter 1) without touching production code.

## What is a Bounded Context?

A **bounded context** is an explicit boundary where a particular domain model and **ubiquitous language** apply. Inside the boundary, terms have a precise meaning. The same English word can mean something completely different in another context — and that is normal.

In a monolith, contexts often live in separate Java packages (or modules). In microservices, each context typically becomes its own deployable service.

```
┌─────────────────────┐     ┌─────────────────────┐     ┌─────────────────────┐
│  CATALOG context    │     │  SALES context      │     │  SHIPPING context   │
│                     │     │                     │     │                     │
│  CatalogProduct     │     │  Customer           │     │  DeliveryRecipient  │
│  list price         │     │  Order              │     │  Shipment           │
│  description        │     │  line item snapshot │     │  delivery address   │
│                     │     │                     │     │                     │
│  "What can we sell  │     │  "What did the      │     │  "Where do we send  │
│   right now?"       │     │   buyer commit to?" │     │   the boxes?"       │
└──────────┬──────────┘     └──────────┬──────────┘     └──────────┬──────────┘
           │                           │                           │
           │  CatalogToSalesTranslator │  SalesToShippingTranslator│
           └──────────────────────────►└──────────────────────────►
                    (copy snapshot)              (copy pick list + address)
```

### Key ideas in this example

| Concept | How it appears in code |
|---------|------------------------|
| Separate models for the same word | `CatalogProduct` vs price snapshot on `OrderLineItem` — both called "product" in conversation, different classes |
| Ubiquitous language per context | Sales says **Customer**; Shipping says **Recipient** (gift orders need both) |
| Context boundary | `customer/` + `order/` = Sales; `catalog/` = Catalog; `shipping/` = Shipping |
| Cross-context reference | `Shipment.orderId` is a plain `Long`, not `@ManyToOne Order` |
| Translation at the edge | `CatalogToSalesTranslator`, `SalesToShippingTranslator` in `integration/` |

**Start here:** run `BoundedContextTest` (pure domain, no Spring), then `BoundedContextIntegrationTest` to see all three contexts persist side by side.

## What is an Aggregate?

An **aggregate** is a cluster of domain objects treated as a single unit for data changes. One object is the **aggregate root** — the only entry point for modifications.

```
┌─────────────────────────────────────────────┐
│  Order Aggregate (consistency boundary)     │
│                                             │
│   Order  ←── aggregate root                 │
│     │                                       │
│     ├── OrderLineItem  (local entity)       │
│     └── OrderLineItem  (local entity)       │
│                                             │
│   customerId: 42  ──reference by id only──► │
└─────────────────────────────────────────────┘
                                              │
                              ┌───────────────▼──────────────┐
                              │  Customer Aggregate (separate)│
                              │    Customer                   │
                              └──────────────────────────────┘
```

## Key ideas in this example

| Concept | How it appears in code |
|---------|------------------------|
| Aggregate root | `Order` — all mutations go through it |
| Local entity | `OrderLineItem` — package-private, no public repository |
| Encapsulation | `lineItems()` returns immutable `OrderLineItemView` records |
| Cross-aggregate reference | `CustomerId` embedded in `Order`, not `@ManyToOne Customer` |
| Invariants | Empty orders cannot be placed; only drafts can be edited |

## Run the tests

```bash
cd ddd-learning
mvn test
```

Suggested order:

1. `BoundedContextTest` — why we split models (Chapter 1)
2. `OrderAggregateTest` — aggregate boundaries within Sales
3. `BoundedContextIntegrationTest` — Catalog → Sales → Shipping flow
4. `OrderAggregatePersistenceTest` — JPA saves the whole Order aggregate through `OrderRepository` only

## Try breaking the rules (learning exercises)

1. Make `OrderLineItem` public and add setters — notice how invariants leak.
2. Replace `CustomerId` with `@ManyToOne Customer` — notice how loading an order can accidentally pull in another aggregate.
3. Add an `OrderLineItemRepository` — notice how line items can be changed without going through `Order`.
