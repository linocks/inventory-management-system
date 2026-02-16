package com.inventory.product.service;

import com.inventory.common.dto.ProductDTO;
import com.inventory.common.exception.ProductNotFoundException;
import com.inventory.common.outbox.OutboxEventService;
import com.inventory.product.entity.Product;
import com.inventory.product.mapper.ProductMapper;
import com.inventory.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static com.inventory.common.constants.KafkaConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private OutboxEventService outboxEventService;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product product;
    private ProductDTO productDTO;

    @BeforeEach
    void setUp() {
        product = Product.builder()
                .id(1L)
                .sku("PROD-001")
                .name("Test Product")
                .description("A test product")
                .category("Electronics")
                .price(new BigDecimal("29.99"))
                .build();

        productDTO = ProductDTO.builder()
                .id(1L)
                .sku("PROD-001")
                .name("Test Product")
                .description("A test product")
                .category("Electronics")
                .price(new BigDecimal("29.99"))
                .initialStock(50)
                .build();
    }

    @Nested
    @DisplayName("createProduct")
    class CreateProduct {

        @Test
        @DisplayName("should create product and save outbox event")
        void shouldCreateProductAndSaveOutboxEvent() {
            when(productMapper.toEntity(productDTO)).thenReturn(product);
            when(productRepository.save(product)).thenReturn(product);
            when(productMapper.toDTO(product)).thenReturn(productDTO);

            ProductDTO result = productService.createProduct(productDTO);

            assertThat(result).isEqualTo(productDTO);
            verify(productRepository).save(product);
            verify(outboxEventService).saveEvent(eq(TOPIC_PRODUCT_CREATED), eq("PROD-001"), any());
        }
    }

    @Nested
    @DisplayName("getProductById")
    class GetProductById {

        @Test
        @DisplayName("should return product when found")
        void shouldReturnProductWhenFound() {
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(productMapper.toDTO(product)).thenReturn(productDTO);

            ProductDTO result = productService.getProductById(1L);

            assertThat(result).isEqualTo(productDTO);
            verify(productRepository).findById(1L);
        }

        @Test
        @DisplayName("should throw ProductNotFoundException when not found")
        void shouldThrowWhenNotFound() {
            when(productRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.getProductById(99L))
                    .isInstanceOf(ProductNotFoundException.class)
                    .hasMessageContaining("99");
        }
    }

    @Nested
    @DisplayName("updateProduct")
    class UpdateProduct {

        @Test
        @DisplayName("should update product and save outbox event")
        void shouldUpdateProductAndSaveOutboxEvent() {
            ProductDTO updateDTO = ProductDTO.builder()
                    .name("Updated Product")
                    .price(new BigDecimal("39.99"))
                    .build();
            ProductDTO updatedDTO = ProductDTO.builder()
                    .id(1L)
                    .sku("PROD-001")
                    .name("Updated Product")
                    .price(new BigDecimal("39.99"))
                    .build();

            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(productRepository.save(product)).thenReturn(product);
            when(productMapper.toDTO(product)).thenReturn(updatedDTO);

            ProductDTO result = productService.updateProduct(1L, updateDTO);

            assertThat(result.getName()).isEqualTo("Updated Product");
            verify(productMapper).updateEntity(updateDTO, product);
            verify(outboxEventService).saveEvent(eq(TOPIC_PRODUCT_UPDATED), eq("PROD-001"), any());
        }
    }

    @Nested
    @DisplayName("deleteProduct")
    class DeleteProduct {

        @Test
        @DisplayName("should delete product and save outbox event")
        void shouldDeleteProductAndSaveOutboxEvent() {
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));

            productService.deleteProduct(1L);

            verify(productRepository).delete(product);
            verify(outboxEventService).saveEvent(eq(TOPIC_PRODUCT_DELETED), eq("PROD-001"), any());
        }
    }
}
