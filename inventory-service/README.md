# Inventory Service

Manages stock levels, consumes product lifecycle events from Kafka, and pushes real-time stock updates via WebSocket.

## Port

`8082`

## API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/v1/inventory/{sku}` | Get stock level for a product |
| GET | `/api/v1/inventory` | Get all stock levels (paginated) |
| PUT | `/api/v1/inventory/{sku}/restock` | Add stock (supplier delivery) |
| PUT | `/api/v1/inventory/{sku}/sell` | Deduct stock (sale) |
| PUT | `/api/v1/inventory/{sku}/adjust` | Set exact stock quantity |
| POST | `/api/v1/outbox/admin/replay` | Replay dead outbox events |
| POST | `/api/v1/outbox/admin/reconcile` | Reconcile stale outbox events |

## WebSocket

**Endpoint:** `/ws/inventory` (STOMP over SockJS)

| Topic | Description |
|---|---|
| `/topic/inventory` | All stock updates (broadcast) |
| `/topic/inventory/{sku}` | Updates for a specific product |

## Kafka

### Consumed Topics

| Topic | Source | Action |
|---|---|---|
| `inventory.product.created` | Product Service | Creates stock record with initial quantity |
| `inventory.product.updated` | Product Service | Acknowledges product changes |
| `inventory.product.deleted` | Product Service | Removes stock record |

Consumer group: `inventory-service-group` (3 concurrent listeners)

### Produced Topics

| Topic | Trigger |
|---|---|
| `inventory.stock.updated` | Any stock change (create, restock, sell, adjust) |

Published via the **Transactional Outbox Pattern**.

## Databases

**PostgreSQL** — `inventory_db`

| Table | Purpose |
|---|---|
| `stock` | Current stock levels (with optimistic locking) |
| `outbox_events` | Outbox for reliable Kafka publishing |
| `processed_events` | Consumer inbox for idempotent event processing |
| `inventory_summary_projection` | Pre-aggregated summary (shared with reporting) |

**MongoDB** — `inventory_events`

| Collection | Purpose |
|---|---|
| `inventoryEvent` | Append-only event history |

## Database Migrations

Schema is managed by **Flyway**. Migrations run automatically on service startup from `src/main/resources/db/migration/`. To add a schema change, create a new file following the naming convention `V{number}__{description}.sql`.

## Key Classes

| Class | Purpose |
|---|---|
| `InventoryController` | REST API endpoints |
| `InventoryServiceImpl` | Stock operations (restock, sell, adjust) |
| `InventoryEventConsumer` | Kafka consumer for product events |
| `StockUpdateNotifier` | WebSocket broadcaster |
| `EventInboxService` | Idempotent event processing via inbox pattern |
| `EventStoreService` | MongoDB event persistence (circuit-broken) |
| `StockMapper` | MapStruct entity/DTO mapping |

## Resilience

- **Optimistic locking** — `@Version` on Stock entity prevents lost updates
- **Retry** — Stock writes retry 3x on version conflicts (100ms delay)
- **Circuit breakers** — MongoDB and WebSocket failures don't block stock operations
- **Rate limiting** — 100 reads/s, 30 writes/s
- **Dead Letter Topics** — Failed Kafka messages routed to `{topic}.DLT` after 3 retries

## Dependencies

- Spring Web, Spring Data JPA, Spring Data MongoDB, Spring Kafka, Spring WebSocket
- PostgreSQL driver
- SpringDoc OpenAPI (Swagger UI at `/swagger-ui.html`)
- Resilience4j (rate limiting, circuit breakers, retries)
- Caffeine cache (1000 entries, 5min TTL)
- Testcontainers (integration tests)

## Running

```bash
# Requires PostgreSQL, MongoDB, and Kafka running
./gradlew :inventory-service:bootRun
```
