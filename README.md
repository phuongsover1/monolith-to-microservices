# E-Commerce Monolith

A small Spring Boot e-commerce application designed as a **starting monolith** for learning [*Monolith to Microservices*](https://samnewman.io/books/monolith-to-microservices/) by Sam Newman.

Three bounded contexts live in **one application** and **one database**:

| Context   | Package              | Responsibility                          |
|-----------|----------------------|-----------------------------------------|
| User      | `com.ecommerce.user` | Customer accounts                       |
| Inventory | `com.ecommerce.inventory` | Product catalog and stock        |
| Order     | `com.ecommerce.order` | Checkout, order lifecycle          |

Cross-context calls are in-process today (e.g. `OrderService` calls `UserService` and `InventoryService`). Foreign keys tie `orders` → `users` and `order_items` → `products`. That coupling is intentional—it gives you something concrete to split later.

## Prerequisites

- Java 21+
- Maven 3.9+

## Run locally (H2 in-memory)

```bash
mvn spring-boot:run
```

The app starts on [http://localhost:8080](http://localhost:8080).

H2 console: [http://localhost:8080/h2-console](http://localhost:8080/h2-console)  
JDBC URL: `jdbc:h2:mem:ecommerce`

Seed data includes two users and three products (see `V2__seed_data.sql`).

## Run with PostgreSQL

```bash
# example with Docker
docker run --name ecommerce-db -e POSTGRES_DB=ecommerce -e POSTGRES_USER=ecommerce -e POSTGRES_PASSWORD=ecommerce -p 5432:5432 -d postgres:16

mvn spring-boot:run -Dspring-boot.run.profiles=postgres
```

## API overview

### Users — `/api/users`

| Method | Path           | Description      |
|--------|----------------|------------------|
| GET    | `/api/users`   | List users       |
| GET    | `/api/users/{id}` | Get user      |
| POST   | `/api/users`   | Register user    |

### Inventory — `/api/inventory/products`

| Method | Path                              | Description        |
|--------|-----------------------------------|--------------------|
| GET    | `/api/inventory/products`         | List products      |
| GET    | `/api/inventory/products/{id}`    | Get product        |
| POST   | `/api/inventory/products`         | Add product        |
| POST   | `/api/inventory/products/{id}/restock` | Add stock   |

### Orders — `/api/orders`

| Method | Path                         | Description              |
|--------|------------------------------|--------------------------|
| GET    | `/api/orders/{id}`           | Get order                |
| GET    | `/api/orders/user/{userId}`  | List orders for user     |
| POST   | `/api/orders`                | Place order (reserves stock) |
| POST   | `/api/orders/{id}/cancel`    | Cancel order (restores stock) |

### Example: place an order

```bash
curl -s -X POST http://localhost:8080/api/orders \
  -H 'Content-Type: application/json' \
  -d '{
    "userId": 1,
    "items": [
      { "productId": 1, "quantity": 1 },
      { "productId": 2, "quantity": 2 }
    ]
  }' | jq
```

## Project layout

```
src/main/java/com/ecommerce/
├── user/          # User bounded context
├── inventory/     # Inventory bounded context
├── order/         # Order bounded context
└── common/        # Shared exception handling

src/main/resources/db/migration/
├── V1__init_schema.sql
└── V2__seed_data.sql
```

## Ideas for book exercises

As you read *Monolith to Microservices*, you can evolve this repo step by step:

1. **Identify seams** — Note where `OrderService` depends on other packages; draw context boundaries.
2. **Extract Inventory** — Move inventory to its own service; replace in-process calls with HTTP or messaging.
3. **Split the database** — Give each service its own schema/DB; replace FK joins with IDs and eventual consistency.
4. **Strangler fig** — Put a gateway in front and route traffic gradually to new services.
5. **Distributed transactions** — Replace the monolith’s single `@Transactional` order flow with sagas or outbox patterns.

## Tests

```bash
mvn test
```

`OrderFlowIntegrationTest` verifies that placing an order reduces product stock in the shared database.
