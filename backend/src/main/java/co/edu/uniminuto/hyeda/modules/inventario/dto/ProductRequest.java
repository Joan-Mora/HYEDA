package co.edu.uniminuto.hyeda.modules.inventario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank String name,
        String description,
        @NotNull BigDecimal dailyPrice,
        @NotNull Integer stockTotal,
        Integer minStockThreshold,
        Boolean active
) {}
