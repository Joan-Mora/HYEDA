package co.edu.uniminuto.hyeda.catalog.dto;

import co.edu.uniminuto.hyeda.catalog.domain.StockMovementType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class ProductDtos {

    private ProductDtos() {
    }

    public record ProductView(
            Long id,
            String sku,
            String name,
            String description,
            String category,
            String unit,
            BigDecimal dailyPrice,
            BigDecimal lateFeePerDay,
            BigDecimal replacementValue,
            Integer stockTotal,
            Integer stockAvailable,
            Integer stockInUse,
            Integer minStockThreshold,
            boolean lowStock,
            boolean outOfStock,
            boolean active,
            LocalDateTime createdAt
    ) {
    }

    public record SaveProductRequest(
            @Size(max = 40) String sku,
            @NotBlank(message = "El nombre del equipo es obligatorio")
            @Size(max = 200) String name,
            String description,
            @Size(max = 80) String category,
            String unit,
            @NotNull(message = "El precio por dia es obligatorio")
            @DecimalMin(value = "0.0", message = "El precio no puede ser negativo") BigDecimal dailyPrice,
            @DecimalMin(value = "0.0", message = "El recargo no puede ser negativo") BigDecimal lateFeePerDay,
            @DecimalMin(value = "0.0", message = "El valor de reposicion no puede ser negativo")
            BigDecimal replacementValue,
            @NotNull(message = "El stock total es obligatorio")
            @Min(value = 0, message = "El stock no puede ser negativo") Integer stockTotal,
            @Min(value = 0, message = "El umbral no puede ser negativo") Integer minStockThreshold,
            Boolean active
    ) {
    }

    public record AdjustStockRequest(
            @NotNull(message = "El nuevo total es obligatorio")
            @Min(value = 0, message = "El stock no puede ser negativo") Integer newTotal,
            @NotBlank(message = "Indica por que se ajusta el inventario") String reason
    ) {
    }

    public record StockLedgerView(
            Long id,
            Long productId,
            Long rentalId,
            StockMovementType movementType,
            Integer quantity,
            Integer balanceAfter,
            String note,
            LocalDateTime createdAt
    ) {
    }

    /** Disponibilidad consultada desde el wizard antes de armar el carrito. */
    public record AvailabilityView(Long productId, String name, Integer stockAvailable, BigDecimal dailyPrice) {
    }
}
