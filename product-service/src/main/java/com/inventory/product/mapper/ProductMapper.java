package com.inventory.product.mapper;

import com.inventory.common.dto.ProductDTO;
import com.inventory.product.entity.Product;
import org.mapstruct.*;

/**
 * Maps between {@link Product} entities and {@link ProductDTO} data transfer objects.
 */
@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface ProductMapper {

    @Mapping(target = "initialStock", ignore = true)
    ProductDTO toDTO(Product product);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "modifiedBy", ignore = true)
    Product toEntity(ProductDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sku", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "modifiedBy", ignore = true)
    void updateEntity(ProductDTO dto, @MappingTarget Product product);
}
