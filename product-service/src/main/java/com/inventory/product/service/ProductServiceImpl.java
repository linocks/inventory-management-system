package com.inventory.product.service;

import com.inventory.common.dto.ProductDTO;
import com.inventory.common.event.ProductCreatedEvent;
import com.inventory.common.event.ProductDeletedEvent;
import com.inventory.common.event.ProductUpdatedEvent;
import com.inventory.common.exception.ProductNotFoundException;
import com.inventory.common.outbox.OutboxEventService;
import com.inventory.product.entity.Product;
import com.inventory.product.mapper.ProductMapper;
import com.inventory.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.inventory.common.constants.KafkaConstants.*;

/**
 * Implementation of {@link ProductService}.
 * Publishes Kafka events via the transactional outbox on create, update, and delete.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final OutboxEventService outboxEventService;

    @Override
    @Transactional
    public ProductDTO createProduct(ProductDTO productDTO) {
        Product product = productMapper.toEntity(productDTO);
        Product saved = productRepository.save(product);
        log.info("Product created: sku={}", saved.getSku());

        ProductCreatedEvent event = ProductCreatedEvent.builder()
                .productId(saved.getId())
                .sku(saved.getSku())
                .name(saved.getName())
                .category(saved.getCategory())
                .price(saved.getPrice())
                .initialStock(productDTO.getInitialStock())
                .build();
        outboxEventService.saveEvent(TOPIC_PRODUCT_CREATED, saved.getSku(), event);

        return productMapper.toDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "#id")
    public ProductDTO getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        return productMapper.toDTO(product);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "#sku")
    public ProductDTO getProductBySku(String sku) {
        Product product = productRepository.findBySku(sku)
                .orElseThrow(() -> new ProductNotFoundException(sku));
        return productMapper.toDTO(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductDTO> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable)
                .map(productMapper::toDTO);
    }

    @Override
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public ProductDTO updateProduct(Long id, ProductDTO productDTO) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        productMapper.updateEntity(productDTO, product);
        Product updated = productRepository.save(product);
        log.info("Product updated: sku={}", updated.getSku());

        ProductUpdatedEvent event = ProductUpdatedEvent.builder()
                .productId(updated.getId())
                .sku(updated.getSku())
                .name(updated.getName())
                .category(updated.getCategory())
                .price(updated.getPrice())
                .build();
        outboxEventService.saveEvent(TOPIC_PRODUCT_UPDATED, updated.getSku(), event);

        return productMapper.toDTO(updated);
    }

    @Override
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        productRepository.delete(product);
        log.info("Product deleted: sku={}", product.getSku());

        ProductDeletedEvent event = ProductDeletedEvent.builder()
                .productId(product.getId())
                .sku(product.getSku())
                .build();
        outboxEventService.saveEvent(TOPIC_PRODUCT_DELETED, product.getSku(), event);
    }
}
