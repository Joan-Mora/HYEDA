package co.edu.uniminuto.hyeda.modules.alquileres.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RentalItemRequest(
        @NotNull Long productId,
        @NotNull @Min(1) Integer quantity
) {}
