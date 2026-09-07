package co.edu.uniminuto.hyeda.modules.alquileres.dto;

import java.math.BigDecimal;

public record RentalItemDto(
        Long id,
        Long productId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        Integer estimatedDays,
        Integer actualDays
) {}
