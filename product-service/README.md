# Product Service

Manages the product catalog and publishes lifecycle events to Kafka for downstream services.

## Port

`8081`

## API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/v1/products` | Create a product |
| GET | `/api/v1/products` | List all products (paginated) |
| GET | `/api/v1/products/{id}` | Get product by ID |
| GET | `/api/v1/products/sku/{sku}` | Get product by SKU |
| PUT | `/api/v1/products/{id}` | Update a product |
| DELETE | `/api/v1/products/{id}` | Delete a product |
| POST | `/api/v1/outbox/admin/replay` | Replay dead outbox events |
| POST | `/api/v1/outbox/admin/reconcile` | Reconcile stale outbox events |

## Kafka Events Produced

| Topic | Trigger |
|---|---|
| `inventory.product.created` | Product created via POST |
| `inventory.product.updated` | Product updated via PUT |
| `inventory.product.deleted` | Product deleted via DELETE |

Events are published via the **Transactional Outbox Pattern** — saved to the `outbox_events` table in the same transaction as the product change, then asynchronously published to Kafka by a poller.

## Database

**PostgreSQL** — `product_db`

| Table | Purpose |
|---|---|
| `products` | Product catalog |
| `outbox_events` | Outbox for reliable Kafka publishing |

## Database Migrations

Schema is managed by **Flyway**. Migrations run automatically on service startup from `src/main/resources/db/migration/`. To add a schema change, create a new file following the naming convention `V{number}__{description}.sql`.

## Key Classes

| Class | Purpose |
|---|---|
| `ProductController` | REST API endpoints |
| `ProductServiceImpl` | Business logic, outbox event creation |
| `ProductMapper` | MapStruct entity/DTO mapping |
| `KafkaProducerConfig` | Kafka topic creation (3 partitions each) |

## Dependencies

- Spring Web, Spring Data JPA, Spring Kafka
- PostgreSQL driver
- SpringDoc OpenAPI (Swagger UI at `/swagger-ui.html`)
- Resilience4j (rate limiting: 100 reads/s, 30 writes/s)
- Caffeine cache (1000 entries, 5min TTL)

## Running

```bash
# Requires PostgreSQL and Kafka running
./gradlew :product-service:bootRun
```
