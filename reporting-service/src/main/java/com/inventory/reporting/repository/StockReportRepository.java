package com.inventory.reporting.repository;

import com.inventory.reporting.entity.StockView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Read-only Spring Data JPA repository for stock data, used by the reporting service.
 */
@Repository
public interface StockReportRepository extends JpaRepository<StockView, Long> {

    @Query("SELECT s FROM StockView s WHERE s.quantity <= s.minThreshold")
    List<StockView> findLowStockProducts();

    @Query("SELECT s FROM StockView s WHERE s.quantity <= s.minThreshold")
    Page<StockView> findLowStockProducts(Pageable pageable);

    @Query("SELECT s FROM StockView s WHERE s.quantity = 0")
    List<StockView> findOutOfStockProducts();

    @Query("SELECT COALESCE(SUM(s.quantity), 0) FROM StockView s")
    long sumTotalStockUnits();

    @Query("SELECT COUNT(s) FROM StockView s WHERE s.quantity <= s.minThreshold")
    long countLowStockProducts();

    @Query("SELECT COUNT(s) FROM StockView s WHERE s.quantity = 0")
    long countOutOfStockProducts();
}
