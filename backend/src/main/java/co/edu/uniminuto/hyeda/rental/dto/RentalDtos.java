package co.edu.uniminuto.hyeda.rental.dto;

import co.edu.uniminuto.hyeda.rental.domain.DocumentPolicy;
import co.edu.uniminuto.hyeda.rental.domain.IncidentCondition;
import co.edu.uniminuto.hyeda.rental.domain.RentalStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class RentalDtos {

    private RentalDtos() {
    }

    // ---------------------------------------------------------------- entrada

    public record RentalItemRequest(
            @NotNull(message = "Selecciona un equipo") Long productId,
            @NotNull @Min(value = 1, message = "La cantidad minima es 1") Integer quantity
    ) {
    }

    public record CreateRentalRequest(
            @NotNull(message = "Selecciona el cliente") Long customerId,
            @NotNull(message = "La fecha de entrega es obligatoria") LocalDate startDate,
            @NotNull(message = "La fecha de devolucion es obligatoria") LocalDate endDate,
            @NotEmpty(message = "Agrega al menos un equipo") @Valid List<RentalItemRequest> items,
            @NotBlank(message = "La direccion de entrega es obligatoria") @Size(max = 500)
            String deliveryAddress,
            String deliveryReference,
            Double deliveryLat,
            Double deliveryLng,
            Boolean haulingEnabled,
            @DecimalMin(value = "0.0", message = "El acarreo no puede ser negativo") BigDecimal haulingCost,
            @DecimalMin(value = "0.0", message = "El descuento no puede ser negativo") BigDecimal discountAmount,
            String discountReason,
            DocumentPolicy documentPolicy,
            String notes,
            @NotNull(message = "Debes registrar la aceptacion de tratamiento de datos")
            Boolean dataConsentAccepted,
            /** Clave del wizard, para reutilizar el stock ya apartado en el paso de equipos. */
            String sessionKey
    ) {
        @AssertTrue(message = "El cliente debe aceptar el tratamiento de datos")
        public boolean isConsentAccepted() {
            return Boolean.TRUE.equals(dataConsentAccepted);
        }
    }

    public record UpdateRentalRequest(
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate,
            @NotEmpty @Valid List<RentalItemRequest> items,
            @NotBlank String deliveryAddress,
            String deliveryReference,
            Double deliveryLat,
            Double deliveryLng,
            Boolean haulingEnabled,
            BigDecimal haulingCost,
            BigDecimal discountAmount,
            String discountReason,
            DocumentPolicy documentPolicy,
            String notes
    ) {
    }

    public record ExtensionRequest(
            @NotNull @Min(value = 1, message = "La extension minima es 1 dia") Integer extraDays,
            String reason
    ) {
    }

    public record IncidentRequest(
            Long rentalItemId,
            @NotNull(message = "Indica el tipo de novedad") IncidentCondition itemCondition,
            @NotNull @Min(1) Integer quantity,
            @NotBlank(message = "Describe brevemente el motivo") String reason,
            String description,
            /** Si se omite, se cobra el valor de reposicion del equipo. */
            BigDecimal chargeAmount
    ) {
    }

    public record ReturnRequest(
            @NotNull(message = "La fecha de devolucion es obligatoria") LocalDate returnDate,
            @Valid List<IncidentRequest> incidents,
            String notes
    ) {
    }

    public record ChangeStatusRequest(@NotNull RentalStatus status, String note) {
    }

    /** Cotizacion en vivo mientras el operador arma el carrito. */
    public record QuoteRequest(
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate,
            @NotEmpty @Valid List<RentalItemRequest> items,
            BigDecimal haulingCost,
            BigDecimal discountAmount
    ) {
    }

    // ----------------------------------------------------------------- salida

    public record RentalItemView(
            Long id,
            Long productId,
            String productName,
            Integer quantity,
            Integer quantityReturned,
            BigDecimal unitPrice,
            BigDecimal lateFeePerDay,
            Integer estimatedDays,
            Integer actualDays,
            BigDecimal lineEstimated,
            BigDecimal lineFinal
    ) {
    }

    public record RentalView(
            Long id,
            String code,
            RentalStatus status,
            DocumentPolicy documentPolicy,
            Long customerId,
            String customerName,
            String customerDocument,
            String customerPhone,
            LocalDate startDate,
            LocalDate endDate,
            LocalDate actualReturnDate,
            Integer estimatedDays,
            Integer actualDays,
            long overdueDays,
            boolean overdue,
            String deliveryAddress,
            String deliveryReference,
            Double deliveryLat,
            Double deliveryLng,
            String deliveryMapsUrl,
            String deliveryEmbedUrl,
            boolean haulingEnabled,
            BigDecimal haulingCost,
            BigDecimal discountAmount,
            String discountReason,
            BigDecimal itemsSubtotal,
            BigDecimal estimatedTotal,
            BigDecimal lateFeeTotal,
            BigDecimal incidentChargeTotal,
            BigDecimal finalAmount,
            BigDecimal currentAmount,
            BigDecimal paidAmount,
            BigDecimal pendingAmount,
            BigDecimal refundAmount,
            BigDecimal creditApplied,
            boolean signed,
            String notes,
            LocalDateTime createdAt,
            LocalDateTime closedAt,
            List<RentalItemView> items,
            List<StatusHistoryView> history,
            List<ExtensionView> extensions,
            List<IncidentView> incidents
    ) {
    }

    public record StatusHistoryView(RentalStatus fromStatus, RentalStatus toStatus, String note,
                                    LocalDateTime changedAt) {
    }

    public record ExtensionView(Long id, Integer extraDays, LocalDate previousEnd, LocalDate newEnd,
                                String reason, LocalDateTime requestedAt) {
    }

    public record IncidentView(Long id, Long rentalItemId, String productName, IncidentCondition itemCondition,
                               Integer quantity, String reason, String description, BigDecimal chargeAmount,
                               boolean hasEvidence, LocalDateTime reportedAt) {
    }

    /** Fila del listado; mas liviana que la ficha completa. */
    public record RentalRow(
            Long id,
            String code,
            RentalStatus status,
            DocumentPolicy documentPolicy,
            Long customerId,
            String customerName,
            String customerDocument,
            LocalDate startDate,
            LocalDate endDate,
            LocalDate actualReturnDate,
            BigDecimal amount,
            BigDecimal pendingAmount,
            Integer itemCount,
            boolean overdue,
            long overdueDays,
            LocalDateTime createdAt
    ) {
    }

    public record QuoteView(
            Integer days,
            BigDecimal itemsSubtotal,
            BigDecimal hauling,
            BigDecimal discount,
            BigDecimal total,
            List<QuoteLine> lines
    ) {
    }

    public record QuoteLine(Long productId, String productName, Integer quantity, BigDecimal unitPrice,
                            Integer availableStock, BigDecimal lineTotal) {
    }

    /**
     * Lo que el wizard necesita saber del cliente en el primer paso para
     * decidir si le exige contrato.
     */
    public record RentalEligibility(
            Long customerId,
            String customerName,
            String customerType,
            BigDecimal reputationScore,
            Integer rentalCount,
            boolean canSkipContract,
            boolean hasIdCopy,
            boolean hasDataConsent,
            BigDecimal openDebt,
            BigDecimal availableCredit,
            DocumentPolicy suggestedPolicy,
            List<String> warnings
    ) {
    }
}
