# Event-Driven Inventory Management System — Implementation Plan

## Context
Take-home project: Design and implement an event-driven inventory management system for a large retail store. The system handles real-time inventory updates via Kafka, uses PostgreSQL for relational data and MongoDB for event storage, and is fully containerized with Docker.

## Tech Stack
- **Language**: Java 21
- **Framework**: Spring Boot 3.2.x
- **Event Handling**: Spring for Apache Kafka (KafkaTemplate + @KafkaListener)
- **Real-time Push**: WebSocket (STOMP over SockJS)
- **Databases**: PostgreSQL (products, stock) + MongoDB (event store, inventory history)
- **Build Tool**: Gradle (Groovy DSL)
- **API Docs**: SpringDoc OpenAPI (Swagger UI)
- **Testing**: JUnit 5, Mockito, Testcontainers, Embedded Kafka
- **Containerization**: Docker + Docker Compose
- **CI/CD**: GitHub Actions

---

## Project Structure

```
inventory-management/
├── settings.gradle
├── build.gradle                          # Root build config, shared dependencies
├── gradle/
│   └── libs.versions.toml                # Version catalog
├── common/
│   ├── build.gradle
│   └── src/main/java/com/inventory/common/
│       ├── dto/
│       │   ├── ProductDTO.java
│       │   ├── StockUpdateDTO.java
│       │   └── ApiResponse.java
│       ├── event/
│       │   ├── BaseEvent.java            # Abstract base: eventId, type, timestamp, payload
│       │   ├── ProductCreatedEvent.java
│       │   ├── ProductUpdatedEvent.java
│       │   ├── ProductDeletedEvent.java
│       │   ├── StockUpdatedEvent.java
│       │   └── EventType.java            # Enum of event types
│       ├── exception/
│       │   ├── ProductNotFoundException.java
│       │   ├── InsufficientStockException.java
│       │   └── GlobalExceptionHandler.java
│       └── constants/
│           └── KafkaConstants.java       # Topic names, consumer groups
│
├── product-service/
│   ├── build.gradle
│   └── src/
│       ├── main/
│       │   ├── java/com/inventory/product/
│       │   │   ├── ProductServiceApplication.java
│       │   │   ├── controller/
│       │   │   │   └── ProductController.java
│       │   │   ├── service/
│       │   │   │   ├── ProductService.java
│       │   │   │   └── ProductServiceImpl.java
│       │   │   ├── repository/
│       │   │   │   └── ProductRepository.java    # JPA
│       │   │   ├── entity/
│       │   │   │   └── Product.java              # JPA entity
│       │   │   ├── kafka/
│       │   │   │   └── ProductEventProducer.java # KafkaTemplate publisher
│       │   │   ├── mapper/
│       │   │   │   └── ProductMapper.java
│       │   │   └── config/
│       │   │       ├── KafkaProducerConfig.java
│       │   │       └── SwaggerConfig.java
│       │   └── resources/
│       │       └── application.yml
│       └── test/java/com/inventory/product/
│           ├── controller/ProductControllerTest.java
│           ├── service/ProductServiceTest.java
│           └── kafka/ProductEventProducerTest.java
│
├── inventory-service/
│   ├── build.gradle
│   └── src/
│       ├── main/
│       │   ├── java/com/inventory/inventory/
│       │   │   ├── InventoryServiceApplication.java
│       │   │   ├── controller/
│       │   │   │   └── InventoryController.java   # Stock endpoints
│       │   │   ├── service/
│       │   │   │   ├── InventoryService.java
│       │   │   │   └── InventoryServiceImpl.java
│       │   │   ├── repository/
│       │   │   │   ├── StockRepository.java        # JPA (PostgreSQL)
│       │   │   │   └── InventoryEventRepository.java # MongoDB
│       │   │   ├── entity/
│       │   │   │   ├── Stock.java                  # JPA entity
│       │   │   │   └── InventoryEvent.java         # MongoDB document
│       │   │   ├── kafka/
│       │   │   │   └── InventoryEventConsumer.java # @KafkaListener
│       │   │   ├── websocket/
│       │   │   │   ├── WebSocketConfig.java
│       │   │   │   └── StockUpdateNotifier.java    # Push real-time updates
│       │   │   └── config/
│       │   │       ├── KafkaConsumerConfig.java
│       │   │       └── MongoConfig.java
│       │   └── resources/
│       │       └── application.yml
│       └── test/java/com/inventory/inventory/
│           ├── kafka/InventoryEventConsumerTest.java
│           ├── service/InventoryServiceTest.java
│           └── integration/InventoryIntegrationTest.java
│
├── reporting-service/
│   ├── build.gradle
│   └── src/
│       ├── main/
│       │   ├── java/com/inventory/reporting/
│       │   │   ├── ReportingServiceApplication.java
│       │   │   ├── controller/
│       │   │   │   └── ReportController.java
│       │   │   ├── service/
│       │   │   │   ├── ReportService.java
│       │   │   │   └── ReportServiceImpl.java
│       │   │   ├── repository/
│       │   │   │   ├── StockReportRepository.java      # PostgreSQL queries
│       │   │   │   └── EventHistoryRepository.java     # MongoDB queries
│       │   │   ├── dto/
│       │   │   │   ├── InventoryReportDTO.java
│       │   │   │   ├── StockLevelDTO.java
│       │   │   │   └── EventHistoryDTO.java
│       │   │   └── config/
│       │   └── resources/
│       │       └── application.yml
│       └── test/java/com/inventory/reporting/
│           ├── controller/ReportControllerTest.java
│           └── service/ReportServiceTest.java
│
├── docker/
│   ├── product-service.Dockerfile
│   ├── inventory-service.Dockerfile
│   ├── reporting-service.Dockerfile
│   └── postgres/
│       └── init.sql                      # Creates databases + tables
├── docker-compose.yml
├── .github/
│   └── workflows/
│       └── ci.yml                        # GitHub Actions pipeline
└── README.md
```

---

## Database Design

### PostgreSQL — `product_db` (used by Product Service)

```sql
CREATE TABLE products (
    id              BIGSERIAL PRIMARY KEY,
    sku             VARCHAR(50) UNIQUE NOT NULL,
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    category        VARCHAR(100),
    price           DECIMAL(10,2) NOT NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### PostgreSQL — `inventory_db` (used by Inventory Service)

```sql
CREATE TABLE stock (
    id              BIGSERIAL PRIMARY KEY,
    product_id      BIGINT NOT NULL,
    sku             VARCHAR(50) UNIQUE NOT NULL,
    quantity         INT NOT NULL DEFAULT 0,
    min_threshold   INT DEFAULT 10,
    last_updated    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_stock_sku ON stock(sku);
CREATE INDEX idx_stock_product_id ON stock(product_id);
```

### MongoDB — `inventory_events` (used by Inventory + Reporting)

```json
{
    "_id": "ObjectId",
    "eventId": "uuid",
    "eventType": "STOCK_UPDATED | PRODUCT_CREATED | ...",
    "sku": "PROD-001",
    "productId": 1,
    "payload": { "event-specific data": "..." },
    "previousQuantity": 100,
    "newQuantity": 85,
    "changeAmount": -15,
    "reason": "SALE | RESTOCK | ADJUSTMENT | RETURN",
    "timestamp": "ISODate",
    "processedAt": "ISODate"
}
```

---

## Kafka Topics

| Topic | Producer | Consumer | Payload |
|---|---|---|---|
| `inventory.product.created` | Product Service | Inventory Service | ProductCreatedEvent (id, sku, name, initialStock) |
| `inventory.product.updated` | Product Service | Inventory Service | ProductUpdatedEvent (id, sku, changedFields) |
| `inventory.product.deleted` | Product Service | Inventory Service | ProductDeletedEvent (id, sku) |
| `inventory.stock.updated` | Inventory Service | Reporting Service | StockUpdatedEvent (sku, prevQty, newQty, reason) |

---

## REST API Endpoints

### Product Service (port 8081)
| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/v1/products` | Create a product |
| GET | `/api/v1/products` | List all products (paginated) |
| GET | `/api/v1/products/{id}` | Get product by ID |
| GET | `/api/v1/products/sku/{sku}` | Get product by SKU |
| PUT | `/api/v1/products/{id}` | Update a product |
| DELETE | `/api/v1/products/{id}` | Delete a product |

### Inventory Service (port 8082)
| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/v1/inventory/{sku}` | Get stock level for a product |
| GET | `/api/v1/inventory` | Get all stock levels (paginated) |
| PUT | `/api/v1/inventory/{sku}/restock` | Restock a product |
| PUT | `/api/v1/inventory/{sku}/sell` | Deduct stock (sale) |
| PUT | `/api/v1/inventory/{sku}/adjust` | Manual stock adjustment |
| WS | `/ws/inventory` | WebSocket endpoint for real-time stock updates |

### Reporting Service (port 8083)
| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/v1/reports/stock-levels` | Current stock levels for all products |
| GET | `/api/v1/reports/low-stock` | Products below minimum threshold |
| GET | `/api/v1/reports/history/{sku}` | Event history for a specific product |
| GET | `/api/v1/reports/history` | All inventory events (paginated, filterable) |
| GET | `/api/v1/reports/summary` | Inventory summary (total products, total value, alerts) |

---

## Implementation Phases (Step-by-Step)

### Phase 1: Project Scaffolding
**What we build:** Root Gradle config, module structure, version catalog, Docker Compose for infrastructure.
- `settings.gradle` — declare all modules
- `build.gradle` (root) — plugins, shared config, subproject defaults
- `gradle/libs.versions.toml` — centralized dependency versions
- `common/build.gradle`
- `product-service/build.gradle`
- `inventory-service/build.gradle`
- `reporting-service/build.gradle`
- `docker-compose.yml` — PostgreSQL, MongoDB, Kafka (KRaft mode, no Zookeeper)
- `docker/postgres/init.sql` — create databases and tables
- Verify: `./gradlew build` compiles, `docker-compose up` starts infrastructure

### Phase 2: Common Module
**What we build:** Shared event classes, DTOs, exceptions, Kafka constants.
- `BaseEvent.java` — abstract class with eventId, eventType, timestamp
- Event subclasses: `ProductCreatedEvent`, `ProductUpdatedEvent`, `ProductDeletedEvent`, `StockUpdatedEvent`
- `EventType.java` enum
- `KafkaConstants.java` — topic names, consumer group IDs
- DTOs: `ProductDTO`, `StockUpdateDTO`, `ApiResponse`
- Exceptions: `ProductNotFoundException`, `InsufficientStockException`
- `GlobalExceptionHandler.java`
- Verify: `./gradlew :common:build` compiles

### Phase 3: Product Service — Entity + CRUD
**What we build:** Product entity, JPA repository, service layer, REST controller, Swagger.
- `Product.java` entity
- `ProductRepository.java` (Spring Data JPA)
- `ProductMapper.java` (entity <-> DTO)
- `ProductService.java` interface + `ProductServiceImpl.java`
- `ProductController.java` with all CRUD endpoints
- `application.yml` — PostgreSQL config, server port 8081
- `SwaggerConfig.java` — OpenAPI setup
- Verify: Start service, test CRUD via Swagger UI at `http://localhost:8081/swagger-ui.html`

### Phase 4: Product Service — Kafka Producer
**What we build:** Kafka producer that publishes events when products are created/updated/deleted.
- `KafkaProducerConfig.java` — KafkaTemplate bean with JSON serializer
- `ProductEventProducer.java` — publishes events to Kafka topics
- Wire producer into `ProductServiceImpl` — publish after each CRUD operation
- Verify: Create a product via REST -> see event in Kafka topic (use `kafka-console-consumer`)

### Phase 5: Inventory Service — Kafka Consumer + Stock Management
**What we build:** Kafka consumer that listens for product events, stock entity, stock management logic.
- `Stock.java` JPA entity
- `StockRepository.java` (Spring Data JPA)
- `InventoryEvent.java` MongoDB document
- `InventoryEventRepository.java` (Spring Data MongoDB)
- `KafkaConsumerConfig.java` — consumer factory with JSON deserializer
- `InventoryEventConsumer.java` — @KafkaListener for product events
- `InventoryService.java` + `InventoryServiceImpl.java` — stock CRUD, event storage
- `InventoryController.java` — restock, sell, adjust endpoints
- `application.yml` — PostgreSQL + MongoDB + Kafka config, port 8082
- Verify: Create product in Product Service -> stock record auto-created in Inventory Service. Call sell/restock endpoints -> stock updated + event stored in MongoDB.

### Phase 6: Inventory Service — WebSocket Real-Time Updates
**What we build:** WebSocket endpoint for pushing stock updates to clients in real-time.
- `WebSocketConfig.java` — STOMP over SockJS configuration
- `StockUpdateNotifier.java` — uses `SimpMessagingTemplate` to broadcast stock changes
- Wire notifier into `InventoryServiceImpl` — notify on every stock change
- Verify: Connect WebSocket client -> perform stock update -> receive push notification

### Phase 7: Reporting Service
**What we build:** Reporting endpoints that query both PostgreSQL and MongoDB.
- Report DTOs: `InventoryReportDTO`, `StockLevelDTO`, `EventHistoryDTO`
- `StockReportRepository.java` — read-only PostgreSQL queries
- `EventHistoryRepository.java` — MongoDB queries with filters
- `ReportService.java` + `ReportServiceImpl.java`
- `ReportController.java` — all reporting endpoints
- `application.yml` — both DB configs, port 8083
- Verify: Generate data via Product + Inventory services -> query reports

### Phase 8: Unit Tests
**What we build:** Unit tests for each service layer and controller.
- Product Service: `ProductServiceTest.java`, `ProductControllerTest.java` (MockMvc), `ProductEventProducerTest.java`
- Inventory Service: `InventoryServiceTest.java`, `InventoryEventConsumerTest.java`
- Reporting Service: `ReportServiceTest.java`, `ReportControllerTest.java`
- Verify: `./gradlew test` — all pass

### Phase 9: Integration Tests
**What we build:** End-to-end tests with Testcontainers + Embedded Kafka.
- `InventoryIntegrationTest.java` — full flow: create product -> event published -> stock created -> stock update -> event stored
- Verify: `./gradlew integrationTest` — all pass

### Phase 10: Docker + CI/CD + Documentation
**What we build:** Dockerfiles, finalize Docker Compose, GitHub Actions, README.
- Dockerfiles for each service (multi-stage builds)
- Update `docker-compose.yml` to include application services
- `.github/workflows/ci.yml` — build, test, Docker build
- `README.md` — project overview, setup instructions, API docs link
- Verify: `docker-compose up --build` starts entire system end-to-end

---

## Key Dependencies per Module

### common
- `spring-boot-starter-web` (for DTO validation annotations)
- `lombok`
- `jackson-databind`

### product-service
- `common` (project dependency)
- `spring-boot-starter-web`
- `spring-boot-starter-data-jpa`
- `spring-kafka`
- `postgresql` driver
- `springdoc-openapi-starter-webmvc-ui`
- `lombok`
- Test: `spring-boot-starter-test`, `spring-kafka-test`

### inventory-service
- `common` (project dependency)
- `spring-boot-starter-web`
- `spring-boot-starter-data-jpa`
- `spring-boot-starter-data-mongodb`
- `spring-boot-starter-websocket`
- `spring-kafka`
- `postgresql` driver
- `springdoc-openapi-starter-webmvc-ui`
- `lombok`
- Test: `spring-boot-starter-test`, `spring-kafka-test`, `testcontainers`

### reporting-service
- `common` (project dependency)
- `spring-boot-starter-web`
- `spring-boot-starter-data-jpa`
- `spring-boot-starter-data-mongodb`
- `postgresql` driver
- `springdoc-openapi-starter-webmvc-ui`
- `lombok`
- Test: `spring-boot-starter-test`, `testcontainers`

---

## Verification (End-to-End Test Plan)
1. `docker-compose up` — all infrastructure starts
2. Start all 3 services (or via Docker Compose)
3. POST a product to Product Service -> verify Kafka event published
4. Verify Inventory Service auto-creates stock record
5. PUT restock via Inventory Service -> verify stock updated + event in MongoDB
6. PUT sell via Inventory Service -> verify stock deducted + WebSocket notification
7. GET reports from Reporting Service -> verify stock levels and event history
8. Run `./gradlew test` — all unit tests pass
9. Run integration tests -> full flow verified
10. `docker-compose up --build` — entire system runs in containers
