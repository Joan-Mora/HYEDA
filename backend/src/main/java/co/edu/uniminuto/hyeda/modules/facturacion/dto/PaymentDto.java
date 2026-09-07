package co.edu.uniminuto.hyeda.modules.facturacion.dto;

import co.edu.uniminuto.hyeda.modules.facturacion.entity.PaymentStatus;
import co.edu.uniminuto.hyeda.modules.facturacion.entity.PaymentType;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentDto(
        Long id,
        Long rentalId,
        PaymentType paymentType,
        BigDecimal amount,
        PaymentStatus status,
        String reference,
        Instant paidAt
) {}
