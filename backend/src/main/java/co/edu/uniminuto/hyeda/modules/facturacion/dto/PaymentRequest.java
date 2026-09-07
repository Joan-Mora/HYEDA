package co.edu.uniminuto.hyeda.modules.facturacion.dto;

import co.edu.uniminuto.hyeda.modules.facturacion.entity.PaymentType;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PaymentRequest(
        @NotNull Long rentalId,
        @NotNull PaymentType paymentType,
        @NotNull BigDecimal amount,
        String reference
) {}
