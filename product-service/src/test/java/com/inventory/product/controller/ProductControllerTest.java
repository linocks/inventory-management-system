package com.inventory.product.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.common.dto.ProductDTO;
import com.inventory.common.exception.GlobalExceptionHandler;
import com.inventory.common.exception.ProductNotFoundException;
import com.inventory.product.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductController productController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ProductDTO productDTO;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(productController)
                .setControllerAdvice(new GlobalExceptionHandler())
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
    @DisplayName("POST /api/v1/products")
    class CreateProduct {

        @Test
        @DisplayName("should create product and return 201")
        void shouldCreateProduct() throws Exception {
            when(productService.createProduct(any(ProductDTO.class))).thenReturn(productDTO);

            mockMvc.perform(post("/api/v1/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(productDTO)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Product created successfully"))
                    .andExpect(jsonPath("$.data.sku").value("PROD-001"))
                    .andExpect(jsonPath("$.data.name").value("Test Product"));
        }

        @Test
        @DisplayName("should return 400 when SKU is blank")
        void shouldReturn400WhenSkuBlank() throws Exception {
            ProductDTO invalidDTO = ProductDTO.builder()
                    .name("Test")
                    .price(new BigDecimal("10.00"))
                    .build();

            mockMvc.perform(post("/api/v1/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidDTO)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/products/{id}")
    class GetProductById {

        @Test
        @DisplayName("should return product when found")
        void shouldReturnProduct() throws Exception {
            when(productService.getProductById(1L)).thenReturn(productDTO);

            mockMvc.perform(get("/api/v1/products/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.sku").value("PROD-001"));
        }

        @Test
        @DisplayName("should return 404 when product not found")
        void shouldReturn404WhenNotFound() throws Exception {
            when(productService.getProductById(99L)).thenThrow(new ProductNotFoundException(99L));

            mockMvc.perform(get("/api/v1/products/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message", containsString("99")));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/products/{id}")
    class UpdateProduct {

        @Test
        @DisplayName("should update product and return 200")
        void shouldUpdateProduct() throws Exception {
            ProductDTO updatedDTO = ProductDTO.builder()
                    .id(1L)
                    .sku("PROD-001")
                    .name("Updated Product")
                    .price(new BigDecimal("39.99"))
                    .build();
            when(productService.updateProduct(eq(1L), any(ProductDTO.class))).thenReturn(updatedDTO);

            mockMvc.perform(put("/api/v1/products/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updatedDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Product updated successfully"))
                    .andExpect(jsonPath("$.data.name").value("Updated Product"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/products/{id}")
    class DeleteProduct {

        @Test
        @DisplayName("should delete product and return 200")
        void shouldDeleteProduct() throws Exception {
            doNothing().when(productService).deleteProduct(1L);

            mockMvc.perform(delete("/api/v1/products/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Product deleted successfully"));
        }
    }
}
