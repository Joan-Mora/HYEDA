package co.edu.uniminuto.hyeda.modules.alquileres.dto;

import co.edu.uniminuto.hyeda.modules.alquileres.entity.DocumentType;
import co.edu.uniminuto.hyeda.modules.alquileres.entity.RentalStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record RentalDto(
        Long id,
        Long customerId,
        String customerName,
        RentalStatus status,
        LocalDate startDate,
        LocalDate endDate,
        LocalDate actualReturnDate,
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
        BigDecimal totalAmount,
        BigDecimal finalAmount,
        Integer estimatedDays,
        Integer actualDays,
        List<RentalItemDto> items
) {}
