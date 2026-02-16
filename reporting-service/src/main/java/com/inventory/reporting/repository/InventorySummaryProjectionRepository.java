package com.inventory.reporting.repository;

import com.inventory.reporting.entity.InventorySummaryProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InventorySummaryProjectionRepository extends JpaRepository<InventorySummaryProjection, Long> {
}
