package com.inventory.inventory.entity;

import com.inventory.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * JPA entity representing the current stock level for a product.
 * Uses {@code @Version} for optimistic locking to prevent lost updates under concurrency.
 */
@Entity
@Table(name = "stock")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Stock extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "stock_seq")
    @SequenceGenerator(name = "stock_seq", sequenceName = "stock_id_seq", allocationSize = 50)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(unique = true, nullable = false, length = 50)
    private String sku;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "min_threshold")
    @Builder.Default
    private int minThreshold = 10;

    @Version
    private Long version;
}
