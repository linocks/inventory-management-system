package com.inventory.inventory.mapper;

import com.inventory.inventory.dto.StockResponseDTO;
import com.inventory.inventory.entity.Stock;
import org.mapstruct.Mapper;

/**
 * Maps between {@link Stock} entities and {@link StockResponseDTO} data transfer objects.
 */
@Mapper(componentModel = "spring")
public interface StockMapper {

    StockResponseDTO toDTO(Stock stock);
}
