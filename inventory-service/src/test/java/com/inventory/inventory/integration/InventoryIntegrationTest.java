package com.inventory.inventory.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.common.dto.ApiResponse;
import com.inventory.common.event.ProductCreatedEvent;
import com.inventory.common.event.ProductDeletedEvent;
import com.inventory.inventory.dto.StockResponseDTO;
import com.inventory.inventory.dto.StockUpdateDTO;
import com.inventory.inventory.entity.InventoryEvent;
import com.inventory.inventory.entity.Stock;
import com.inventory.inventory.repository.InventoryEventRepository;
import com.inventory.inventory.repository.StockRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static com.inventory.common.constants.KafkaConstants.TOPIC_PRODUCT_CREATED;
import static com.inventory.common.constants.KafkaConstants.TOPIC_PRODUCT_DELETED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class InventoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("inventory_db")
            .withUsername("test")
            .withPassword("test");

    @Container
    static MongoDBContainer mongo = new MongoDBContainer(DockerImageName.parse("mongo:7.0"));

    @Container
    static ConfluentKafkaContainer kafka = new ConfluentKafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("outbox.publisher.enabled", () -> "false");
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private InventoryEventRepository eventRepository;

    @LocalServerPort
    private int port;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/v1/inventory";
    }

    @Test
    @Order(1)
    @DisplayName("ProductCreatedEvent via Kafka should create stock record and store event in MongoDB")
    void shouldCreateStockFromKafkaEvent() throws Exception {
        ProductCreatedEvent event = ProductCreatedEvent.builder()
                .productId(1L)
                .sku("INT-TEST-001")
                .name("Integration Test Product")
                .category("Testing")
                .price(new BigDecimal("19.99"))
                .initialStock(100)
                .build();

        String message = objectMapper.writeValueAsString(event);
        kafkaTemplate.send(TOPIC_PRODUCT_CREATED, event.getSku(), message).get();

        // Wait for the Kafka consumer to process the event
        await().atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    Optional<Stock> stock = stockRepository.findBySku("INT-TEST-001");
                    assertThat(stock).isPresent();
                    assertThat(stock.get().getQuantity()).isEqualTo(100);
                    assertThat(stock.get().getProductId()).isEqualTo(1L);
                });

        // Verify event was stored in MongoDB
        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    List<InventoryEvent> events = eventRepository.findAll();
                    assertThat(events).anyMatch(e ->
                            e.getSku().equals("INT-TEST-001") && e.getReason().equals("INITIAL"));
                });
    }

    @Test
    @Order(2)
    @DisplayName("Duplicate ProductCreatedEvent should be ignored (idempotency)")
    void shouldIgnoreDuplicateProductCreatedEvent() throws Exception {
        // Get current stock count
        long countBefore = stockRepository.count();

        ProductCreatedEvent event = ProductCreatedEvent.builder()
                .productId(2L)
                .sku("INT-TEST-DUP")
                .name("Duplicate Test")
                .category("Testing")
                .price(new BigDecimal("9.99"))
                .initialStock(50)
                .build();

        String message = objectMapper.writeValueAsString(event);

        // Send the same event twice
        kafkaTemplate.send(TOPIC_PRODUCT_CREATED, event.getSku(), message).get();
        kafkaTemplate.send(TOPIC_PRODUCT_CREATED, event.getSku(), message).get();

        // Wait for processing
        await().atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    Optional<Stock> stock = stockRepository.findBySku("INT-TEST-DUP");
                    assertThat(stock).isPresent();
                    assertThat(stock.get().getQuantity()).isEqualTo(50);
                });

        // Should only have one stock record for this SKU (not two)
        assertThat(stockRepository.findBySku("INT-TEST-DUP")).isPresent();
    }

    @Test
    @Order(3)
    @DisplayName("GET /api/v1/inventory/{sku} should return stock level")
    void shouldGetStockBySku() {
        ResponseEntity<ApiResponse<StockResponseDTO>> response = restTemplate.exchange(
                baseUrl + "/INT-TEST-001",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData().getSku()).isEqualTo("INT-TEST-001");
        assertThat(response.getBody().getData().getQuantity()).isEqualTo(100);
    }

    @Test
    @Order(4)
    @DisplayName("PUT /api/v1/inventory/{sku}/restock should add stock and store event")
    void shouldRestockProduct() {
        StockUpdateDTO dto = StockUpdateDTO.builder().quantity(50).reason("Supplier delivery").build();

        ResponseEntity<ApiResponse<StockResponseDTO>> response = restTemplate.exchange(
                baseUrl + "/INT-TEST-001/restock",
                HttpMethod.PUT,
                new HttpEntity<>(dto),
                new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().getQuantity()).isEqualTo(150);

        // Verify event stored in MongoDB
        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    List<InventoryEvent> events = eventRepository.findAll();
                    assertThat(events).anyMatch(e ->
                            e.getSku().equals("INT-TEST-001") && e.getReason().equals("RESTOCK"));
                });
    }

    @Test
    @Order(5)
    @DisplayName("PUT /api/v1/inventory/{sku}/sell should deduct stock and store event")
    void shouldSellProduct() {
        StockUpdateDTO dto = StockUpdateDTO.builder().quantity(30).reason("Customer order").build();

        ResponseEntity<ApiResponse<StockResponseDTO>> response = restTemplate.exchange(
                baseUrl + "/INT-TEST-001/sell",
                HttpMethod.PUT,
                new HttpEntity<>(dto),
                new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().getQuantity()).isEqualTo(120);

        // Verify event stored in MongoDB
        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    List<InventoryEvent> events = eventRepository.findAll();
                    assertThat(events).anyMatch(e ->
                            e.getSku().equals("INT-TEST-001") && e.getReason().equals("SALE"));
                });
    }

    @Test
    @Order(6)
    @DisplayName("PUT /api/v1/inventory/{sku}/sell should return 400 when insufficient stock")
    void shouldRejectSaleWhenInsufficientStock() {
        StockUpdateDTO dto = StockUpdateDTO.builder().quantity(9999).build();

        ResponseEntity<ApiResponse<Void>> response = restTemplate.exchange(
                baseUrl + "/INT-TEST-001/sell",
                HttpMethod.PUT,
                new HttpEntity<>(dto),
                new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).contains("Insufficient stock");
    }

    @Test
    @Order(7)
    @DisplayName("PUT /api/v1/inventory/{sku}/adjust should set exact quantity and store event")
    void shouldAdjustStock() {
        StockUpdateDTO dto = StockUpdateDTO.builder().quantity(75).reason("Physical count correction").build();

        ResponseEntity<ApiResponse<StockResponseDTO>> response = restTemplate.exchange(
                baseUrl + "/INT-TEST-001/adjust",
                HttpMethod.PUT,
                new HttpEntity<>(dto),
                new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().getQuantity()).isEqualTo(75);

        // Verify event stored in MongoDB
        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    List<InventoryEvent> events = eventRepository.findAll();
                    assertThat(events).anyMatch(e ->
                            e.getSku().equals("INT-TEST-001") && e.getReason().equals("ADJUSTMENT"));
                });
    }

    @Test
    @Order(8)
    @DisplayName("ProductDeletedEvent via Kafka should remove stock record")
    void shouldRemoveStockFromKafkaEvent() throws Exception {
        // Ensure stock exists before deletion
        assertThat(stockRepository.findBySku("INT-TEST-001")).isPresent();

        ProductDeletedEvent event = ProductDeletedEvent.builder()
                .productId(1L)
                .sku("INT-TEST-001")
                .build();

        String message = objectMapper.writeValueAsString(event);
        kafkaTemplate.send(TOPIC_PRODUCT_DELETED, event.getSku(), message).get();

        // Wait for the Kafka consumer to process the event
        await().atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    Optional<Stock> stock = stockRepository.findBySku("INT-TEST-001");
                    assertThat(stock).isEmpty();
                });
    }
}
