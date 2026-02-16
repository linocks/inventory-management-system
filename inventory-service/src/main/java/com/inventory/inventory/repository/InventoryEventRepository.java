package com.inventory.inventory.repository;

import com.inventory.inventory.entity.InventoryEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Spring Data MongoDB repository for {@link InventoryEvent} documents in the inventory_events collection.
 */
@Repository
public interface InventoryEventRepository extends MongoRepository<InventoryEvent, String> {

    Page<InventoryEvent> findBySku(String sku, Pageable pageable);

    Page<InventoryEvent> findByEventType(String eventType, Pageable pageable);

    List<InventoryEvent> findBySkuAndTimestampBetween(String sku, LocalDateTime from, LocalDateTime to);

    boolean existsByEventId(String eventId);
}
