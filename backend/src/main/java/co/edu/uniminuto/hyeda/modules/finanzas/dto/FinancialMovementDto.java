package co.edu.uniminuto.hyeda.modules.finanzas.dto;

import co.edu.uniminuto.hyeda.modules.finanzas.entity.MovementType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FinancialMovementDto(
        Long id,
        MovementType movementType,
        BigDecimal amount,
        String description,
        String category,
        LocalDate movementDate
) {}
