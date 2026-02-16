package com.inventory.common.exception;

/**
 * Thrown when a sale request exceeds the available stock quantity.
 * Mapped to HTTP 400 by {@link GlobalExceptionHandler}.
 */
public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(String sku, int requested, int available) {
        super("Insufficient stock for SKU: " + sku
                + ". Requested: " + requested
                + ", Available: " + available);
    }
}
