package com.inventory.common.exception;

/**
 * Thrown when a product or stock record cannot be found by ID or SKU.
 * Mapped to HTTP 404 by {@link GlobalExceptionHandler}.
 */
public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(Long id) {
        super("Product not found with id: " + id);
    }

    public ProductNotFoundException(String sku) {
        super("Product not found with SKU: " + sku);
    }
}
