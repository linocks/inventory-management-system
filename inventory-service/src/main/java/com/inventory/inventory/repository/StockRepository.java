package com.inventory.inventory.repository;

import com.inventory.inventory.entity.Stock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Stock} entities in the inventory_db database.
 */
@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {

    Optional<Stock> findBySku(String sku);

    List<Stock> findByQuantityLessThanEqual(int threshold);

    @Query("SELECT s FROM Stock s WHERE s.quantity <= s.minThreshold")
    Page<Stock> findLowStockProducts(Pageable pageable);

    boolean existsBySku(String sku);
}
