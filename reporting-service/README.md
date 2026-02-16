# Reporting Service

Provides inventory reports and real-time dashboard updates. Consumes stock update events from Kafka, maintains a pre-aggregated summary projection, and pushes live updates via WebSocket.

## Port

`8083`

## API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/v1/reports/stock-levels` | Current stock levels for all products (paginated) |
| GET | `/api/v1/reports/low-stock` | Products below minimum threshold (paginated) |
| GET | `/api/v1/reports/history` | All inventory events (paginated) |
| GET | `/api/v1/reports/history/{sku}` | Event history for a specific product (paginated) |
| GET | `/api/v1/reports/summary` | Inventory summary (total products, units, alerts) |

## WebSocket

**Endpoint:** `/ws/reports` (STOMP over SockJS)

| Topic | Description |
|---|---|
| `/topic/reports/stock-levels` | Individual stock level changes (broadcast) |
| `/topic/reports/stock-levels/{sku}` | Updates for a specific product |
| `/topic/reports/summary` | Inventory summary updates |

## Kafka

### Consumed Topics

| Topic | Source | Action |
|---|---|---|
| `inventory.stock.updated` | Inventory Service | Updates summary projection, pushes WebSocket notifications |

Consumer group: `reporting-service-group` (3 concurrent listeners)

## Databases

**PostgreSQL** — `inventory_db` (read-only access to stock table)

| Table | Access | Purpose |
|---|---|---|
| `stock` | Read | Stock levels for reports |
| `inventory_summary_projection` | Read/Write | Pre-aggregated summary counters |

**MongoDB** — `inventory_events` (read-only)

| Collection | Purpose |
|---|---|
| `inventoryEvent` | Event history queries |

## Database Migrations

This service does **not** run Flyway migrations. The `inventory_db` schema is owned and managed by the **inventory-service**. This service connects in read-only mode with `ddl-auto: validate`.

## Key Classes

| Class | Purpose |
|---|---|
| `ReportController` | REST API endpoints |
| `ReportServiceImpl` | Report generation logic |
| `InventorySummaryProjectionService` | Maintains summary projection via incremental deltas |
| `ReportEventConsumer` | Kafka consumer for stock update events |
| `ReportUpdateNotifier` | WebSocket broadcaster for stock levels and summaries |

## Summary Projection

Instead of running expensive `COUNT`/`SUM` queries on every summary request, the service maintains an `inventory_summary_projection` table:

- Updated incrementally when `StockUpdatedEvent` arrives (delta-based)
- Falls back to a full rebuild from source of truth if the projection is missing
- Tracks: `totalProducts`, `totalStockUnits`, `lowStockProducts`, `outOfStockProducts`

## Resilience

- **Circuit breaker** — WebSocket failures don't block event processing
- **Rate limiting** — 100 reads/s
- **Dead Letter Topics** — Failed Kafka messages routed to `{topic}.DLT` after 3 retries
- **Event contract validation** — Rejects unsupported event versions

## Dependencies

- Spring Web, Spring Data JPA, Spring Data MongoDB, Spring Kafka, Spring WebSocket
- PostgreSQL driver
- SpringDoc OpenAPI (Swagger UI at `/swagger-ui.html`)
- Resilience4j (rate limiting, circuit breakers)

## Running

```bash
# Requires PostgreSQL, MongoDB, and Kafka running
./gradlew :reporting-service:bootRun
```
