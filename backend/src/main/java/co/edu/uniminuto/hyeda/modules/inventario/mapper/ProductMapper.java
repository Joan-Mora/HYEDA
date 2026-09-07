package co.edu.uniminuto.hyeda.modules.inventario.mapper;

import co.edu.uniminuto.hyeda.modules.inventario.dto.ProductDto;
import co.edu.uniminuto.hyeda.modules.inventario.dto.ProductRequest;
import co.edu.uniminuto.hyeda.modules.inventario.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(target = "lowStock", expression = "java(entity.getStockAvailable() != null && entity.getMinStockThreshold() != null && entity.getStockAvailable() <= entity.getMinStockThreshold())")
    ProductDto toDto(Product entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "stockAvailable", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Product toEntity(ProductRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "stockAvailable", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void update(@MappingTarget Product entity, ProductRequest request);
}
