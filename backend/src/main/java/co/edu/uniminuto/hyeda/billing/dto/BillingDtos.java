package co.edu.uniminuto.hyeda.billing.dto;

import co.edu.uniminuto.hyeda.billing.domain.PaymentStatus;
import co.edu.uniminuto.hyeda.billing.domain.PaymentType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class BillingDtos {

    private BillingDtos() {
    }

    /** Que hacer con el dinero que el cliente pago de mas. */
    public enum SurplusHandling {
        /** Se le devuelve en efectivo en el mostrador. */
        REEMBOLSO,
        /** Queda como saldo a favor para su proximo alquiler. */
        SALDO_A_FAVOR
    }

    public record CashPaymentRequest(
            @NotNull @Positive BigDecimal amount,
            /** Billete que entrega el cliente; con esto se calcula el vuelto. */
            @PositiveOrZero BigDecimal amountTendered,
            @Size(max = 400) String note
    ) {
    }

    public record TransferPaymentRequest(
            @NotNull @Positive BigDecimal amount,
            @Size(max = 120) String reference,
            @Size(max = 400) String note
    ) {
    }

    public record QrPaymentRequest(
            @NotNull @Positive BigDecimal amount,
            @Size(max = 400) String note
    ) {
    }

    public record ConfirmPaymentRequest(
            @Size(max = 120) String reference,
            @Size(max = 400) String note
    ) {
    }

    public record CreditApplicationRequest(
            @NotNull @Positive BigDecimal amount
    ) {
    }

    /** Cierre del alquiler tras la liquidacion final. */
    public record SettlementRequest(
            SurplusHandling surplusHandling,
            @Size(max = 400) String note
    ) {
    }

    public record PaymentView(
            Long id,
            Long rentalId,
            PaymentType paymentType,
            PaymentStatus status,
            BigDecimal amount,
            BigDecimal amountTendered,
            BigDecimal changeGiven,
            String reference,
            String provider,
            String note,
            /** Solo en pagos por QR: imagen lista para mostrar en pantalla. */
            String qrDataUri,
            String checkoutUrl,
            LocalDateTime paidAt,
            LocalDateTime createdAt
    ) {
    }

    /**
     * Estado de cuenta de un alquiler: es lo que se muestra en el paso de pago
     * del wizard y en la pantalla de cierre.
     */
    public record RentalAccount(
            Long rentalId,
            String rentalCode,
            String status,
            Long customerId,
            String customerName,
            BigDecimal estimatedTotal,
            BigDecimal finalAmount,
            BigDecimal amountDue,
            BigDecimal paid,
            BigDecimal balance,
            /** Positivo cuando el cliente pago de mas y hay que devolverle. */
            BigDecimal surplus,
            BigDecimal creditAvailable,
            BigDecimal creditApplied,
            BigDecimal refundAmount,
            boolean settled,
            boolean fullyPaid,
            List<PaymentView> payments
    ) {
    }
}
