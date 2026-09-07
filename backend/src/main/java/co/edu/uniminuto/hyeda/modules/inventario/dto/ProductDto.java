package co.edu.uniminuto.hyeda.modules.inventario.dto;

import java.math.BigDecimal;

public record ProductDto(
        Long id,
        String name,
        String description,
        BigDecimal dailyPrice,
        Integer stockTotal,
        Integer stockAvailable,
        Integer minStockThreshold,
        Boolean active,
        boolean lowStock
) {}
