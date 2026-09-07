package co.edu.uniminuto.hyeda.modules.finanzas.dto;

import co.edu.uniminuto.hyeda.modules.finanzas.entity.DebtStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DebtDto(
        Long id,
        Long customerId,
        String customerName,
        Long rentalId,
        BigDecimal balance,
        LocalDate dueDate,
        Integer overdueDays,
        DebtStatus status
) {}
