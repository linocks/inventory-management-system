package com.inventory.reporting.repository;

import com.inventory.reporting.entity.InventoryEventView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data MongoDB repository for querying inventory event history in the reporting service.
 */
@Repository
public interface EventHistoryRepository extends MongoRepository<InventoryEventView, String> {

    Page<InventoryEventView> findBySku(String sku, Pageable pageable);

    Page<InventoryEventView> findByEventType(String eventType, Pageable pageable);

    Page<InventoryEventView> findBySkuAndEventType(String sku, String eventType, Pageable pageable);
}
