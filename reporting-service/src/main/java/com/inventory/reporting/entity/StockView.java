package com.inventory.reporting.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "stock")
@Getter
@NoArgsConstructor
public class StockView {

    @Id
    private Long id;

    @Column(name = "product_id")
    private Long productId;

    private String sku;
    private int quantity;

    @Column(name = "min_threshold")
    private int minThreshold;
}
