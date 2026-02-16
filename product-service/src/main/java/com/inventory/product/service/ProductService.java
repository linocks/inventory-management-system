package com.inventory.product.service;

import com.inventory.common.dto.ProductDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for managing products. Implementations publish Kafka events
 * on create, update, and delete operations via the transactional outbox.
 */
public interface ProductService {

    /** Creates a product and publishes a {@code ProductCreatedEvent}. */
    ProductDTO createProduct(ProductDTO productDTO);

    /** Retrieves a product by its database ID. */
    ProductDTO getProductById(Long id);

    /** Retrieves a product by its unique SKU. */
    ProductDTO getProductBySku(String sku);

    /** Returns a paginated list of all products. */
    Page<ProductDTO> getAllProducts(Pageable pageable);

    /** Updates a product and publishes a {@code ProductUpdatedEvent}. */
    ProductDTO updateProduct(Long id, ProductDTO productDTO);

    /** Deletes a product and publishes a {@code ProductDeletedEvent}. */
    void deleteProduct(Long id);
}
