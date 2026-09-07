package co.edu.uniminuto.hyeda.modules.alquileres.dto;

import co.edu.uniminuto.hyeda.modules.alquileres.entity.DocumentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreateRentalRequest(
        @NotNull Long customerId,
        @NotNull LocalDate startDate,
        @NotNull @Min(1) Integer estimatedDays,
        String deliveryAddress,
        String deliveryReference,
        Double deliveryLat,
        Double deliveryLng,
        Boolean haulingEnabled,
        BigDecimal haulingCost,
        BigDecimal discountAmount,
        String discountReason,
        DocumentType documentType,
        Boolean skipContract,
        @NotEmpty @Valid List<RentalItemRequest> items
) {}
