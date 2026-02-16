# Event-Driven Inventory Management System

A microservices-based inventory management system that handles real-time inventory updates via Apache Kafka, using PostgreSQL for relational data and MongoDB for event storage.

## Architecture

```
┌──────────────────┐    Kafka Events     ┌────────────────────┐    Kafka Events     ┌───────────────────┐
│  Product Service │ ──────────────────► │  Inventory Service │ ──────────────────► │ Reporting Service │
│     (port 8081)  │  ProductCreated     │     (port 8082)    │  StockUpdated       │    (port 8083)    │
│                  │  ProductUpdated     │                    │                     │                   │
│  CRUD Products   │  ProductDeleted     │  Stock Management  │                     │  Reports & Alerts │
└──────────────────┘                     └────────────────────┘                     └───────────────────┘
        │                                    │          │                               │          │
        ▼                                    ▼          ▼                               ▼          ▼
   ┌──────────┐                        ┌──────────┐ ┌─────────┐                  ┌──────────┐ ┌─────────┐
   │PostgreSQL│                        │PostgreSQL│ │ MongoDB │                  │PostgreSQL│ │ MongoDB │
   │product_db│                        │inventory │ │ events  │                  │inventory │ │ events  │
   └──────────┘                        └──────────┘ └─────────┘                  └──────────┘ └─────────┘
                                            │                                         │
                                       WebSocket                                 WebSocket
                                       /ws/inventory                             /ws/reports
                                            │                                         │
                                            ▼                                         ▼
                                     ┌─────────────┐                           ┌─────────────┐
                                     │   Clients    │                           │   Clients    │
                                     │ (stock live) │                           │(reports live)│
                                     └─────────────┘                           └─────────────┘
```

## Services

| Service | Port | Description | README |
|---|---|---|---|
| [Product Service](product-service/) | 8081 | Product CRUD, publishes lifecycle events to Kafka | [README](product-service/README.md) |
| [Inventory Service](inventory-service/) | 8082 | Stock management, consumes product events, real-time WebSocket updates | [README](inventory-service/README.md) |
| [Reporting Service](reporting-service/) | 8083 | Reports, event history, real-time WebSocket dashboard updates | [README](reporting-service/README.md) |

## Tech Stack

| Component | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.4.3 |
| Messaging | Apache Kafka (KRaft mode) |
| Real-time Push | WebSocket (STOMP over SockJS) |
| Relational DB | PostgreSQL 16 |
| Event Store | MongoDB 7.0 |
| Resilience | Resilience4j (rate limiting, circuit breakers, retries) |
| Caching | Caffeine (in-process, 5min TTL) |
| Monitoring | Spring Boot Actuator |
| Build Tool | Gradle (Groovy DSL) |
| Object Mapping | MapStruct 1.6 |
| API Docs | SpringDoc OpenAPI (Swagger UI) |
| Database Migrations | Flyway 10.x |
| Testing | JUnit 5, Mockito, Testcontainers |
| Containerization | Docker + Docker Compose |


## Key Design Decisions

- **Transactional Outbox Pattern** — Business data and Kafka events are saved in the same database transaction, then an async poller publishes events to Kafka with lifecycle states (PENDING → IN_PROGRESS → PROCESSED/FAILED/DEAD) and exponential backoff retries.
- **Consumer Inbox Pattern** — Each event carries a unique `eventId`. Consumers register events in a `processed_events` table (unique constraint) within the same transaction as the business operation, providing durable exactly-once processing semantics.
- **Event Contract Versioning** — All events carry a `contractVersion` field. Consumers validate the version before processing and fail fast on unsupported versions, routing to DLT for safe schema evolution.
- **Pre-aggregated Summary Projection** — The reporting service maintains an `inventory_summary_projection` table updated incrementally via event deltas, eliminating expensive full-table scans on summary queries.
- **Dual Database Strategy** — PostgreSQL for ACID-compliant current state (products, stock levels); MongoDB for append-only event history (high write throughput, flexible schema).
- **JPA Auditing** — All entities track `createdAt`, `updatedAt`, `createdBy`, and `modifiedBy` via a shared `BaseEntity`.

## Scalability & Resilience

The system is designed to handle high throughput and degrade gracefully under failure.

| Feature | What It Does |
|---|---|
| **Transactional Outbox** | Reliable event publishing with lifecycle states, async Kafka sends, exponential backoff, and in-flight cap |
| **Consumer Inbox** | Durable exactly-once processing via `processed_events` table with unique `eventId` constraint |
| **Optimistic Locking + Retry** | `@Version` on Stock entity prevents lost updates; writes retry 3x on version conflicts |
| **Circuit Breakers** | MongoDB and WebSocket failures degrade gracefully without blocking stock operations |
| **Rate Limiting** | Resilience4j limits API throughput (100 reads/s, 30 writes/s). Excess requests get HTTP 429 |
| **Kafka Dead Letter Topics** | Failed messages retried 3x, then routed to `{topic}.DLT` for inspection |
| **Idempotent Producers** | Prevents duplicate Kafka messages on producer retries |
| **Consumer Concurrency** | 3 consumer threads per topic (matching partition count) |
| **Connection Pool Tuning** | HikariCP with 20 max connections, 5 minimum idle, explicit timeouts |
| **Targeted Indexes** | Partial indexes for low-stock and out-of-stock queries, outbox publisher performance |
| **Caffeine Caching** | In-process cache (1000 entries, 5min TTL) on reads, evicted on writes |

## Kafka Topics

| Topic | Producer | Consumer | Purpose |
|---|---|---|---|
| `inventory.product.created` | Product Service | Inventory Service | Auto-create stock record |
| `inventory.product.updated` | Product Service | Inventory Service | Acknowledge product changes |
| `inventory.product.deleted` | Product Service | Inventory Service | Remove stock record |
| `inventory.stock.updated` | Inventory Service | Reporting Service | Real-time report updates |

All events carry a `contractVersion` field (currently `1`). Consumers validate the version and reject unknown versions into the DLT.

## Prerequisites

- Java 21
- Docker & Docker Compose
- Gradle 8.x (included via wrapper)

## Quick Start

### 1. Start the entire system with Docker

```bash
docker compose up --build
```

This starts all infrastructure (Kafka, PostgreSQL, MongoDB) and all three services.

### 2. Start infrastructure only (for local development)

```bash
docker compose up kafka postgres mongodb
```

Then start services individually:

```bash
./gradlew :product-service:bootRun
./gradlew :inventory-service:bootRun
./gradlew :reporting-service:bootRun
```

### 3. Stopping

```bash
# Stop and remove containers (keeps database data)
docker compose down

# Stop and wipe all data (clean slate — Flyway re-creates schema on next start)
docker compose down -v
```

### Swagger UI

- Product Service: http://localhost:8081/swagger-ui.html
- Inventory Service: http://localhost:8082/swagger-ui.html
- Reporting Service: http://localhost:8083/swagger-ui.html

## Testing

```bash
# Run all unit tests
./gradlew test -x :inventory-service:test

# Run all tests including integration tests (requires Docker)
./gradlew test

# Run only integration tests
./gradlew :inventory-service:test --tests "*IntegrationTest*"
```

## Project Structure

```
inventory-management/
├── common/                    # Shared events, DTOs, exceptions, outbox, caching
├── product-service/           # Product CRUD + Kafka producer
├── inventory-service/         # Stock management + Kafka consumer/producer + WebSocket
├── reporting-service/         # Reports + Kafka consumer + WebSocket
├── docker/
│   ├── postgres/init.sql      # Database creation (schema managed by Flyway)
│   ├── product-service.Dockerfile
│   ├── inventory-service.Dockerfile
│   └── reporting-service.Dockerfile
├── docker-compose.yml
```
