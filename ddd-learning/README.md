# DDD Learning Sandbox

This module is **isolated** from the main ecommerce monolith. Use it to experiment with Domain-Driven Design concepts from *Monolith to Microservices* (Chapter 1) without touching production code.

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

Start with `OrderAggregateTest` for pure domain behaviour, then `OrderAggregatePersistenceTest` to see how JPA persists the whole aggregate through `OrderRepository` only.

## Try breaking the rules (learning exercises)

1. Make `OrderLineItem` public and add setters — notice how invariants leak.
2. Replace `CustomerId` with `@ManyToOne Customer` — notice how loading an order can accidentally pull in another aggregate.
3. Add an `OrderLineItemRepository` — notice how line items can be changed without going through `Order`.
