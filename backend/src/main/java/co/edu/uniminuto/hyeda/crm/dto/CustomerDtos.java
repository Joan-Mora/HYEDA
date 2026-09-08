package co.edu.uniminuto.hyeda.crm.dto;

import co.edu.uniminuto.hyeda.crm.domain.BackgroundCheckStatus;
import co.edu.uniminuto.hyeda.crm.domain.CustomerType;
import co.edu.uniminuto.hyeda.crm.domain.IdDocumentType;
import co.edu.uniminuto.hyeda.crm.domain.ReputationEventType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class CustomerDtos {

    private CustomerDtos() {
    }

    public record CustomerView(
            Long id,
            String document,
            IdDocumentType documentType,
            String name,
            String phone,
            String email,
            boolean hasIdCopy,
            boolean dataConsent,
            LocalDateTime dataConsentAt,
            BigDecimal reputationScore,
            CustomerType customerType,
            Integer rentalCount,
            boolean canSkipContract,
            String notes,
            boolean active,
            LocalDateTime createdAt,
            List<AddressView> addresses,
            BackgroundCheckView lastBackgroundCheck
    ) {
    }

    public record AddressView(
            Long id,
            String label,
            String address,
            String reference,
            Double lat,
            Double lng,
            String mapsUrl,
            boolean isDefault
    ) {
    }

    public record SaveCustomerRequest(
            @NotBlank(message = "El documento es obligatorio")
            @Pattern(regexp = "^[0-9A-Za-z.\\-]{4,40}$", message = "Documento invalido")
            String document,
            IdDocumentType documentType,
            @NotBlank(message = "El nombre es obligatorio") @Size(max = 180) String name,
            @Pattern(regexp = "^$|^[0-9+ ()\\-]{7,40}$", message = "Telefono invalido") String phone,
            @Email(message = "Correo invalido") String email,
            String notes,
            /**
             * Debe venir en true para poder registrar al cliente: es la
             * aceptacion del tratamiento de datos personales.
             */
            @NotNull(message = "Debes registrar la aceptacion de tratamiento de datos")
            Boolean dataConsent,
            List<SaveAddressRequest> addresses
    ) {
    }

    public record SaveAddressRequest(
            Long id,
            String label,
            @NotBlank(message = "La direccion es obligatoria") String address,
            String reference,
            Double lat,
            Double lng,
            Boolean isDefault
    ) {
    }

    public record ReputationEventView(
            Long id,
            ReputationEventType eventType,
            BigDecimal delta,
            BigDecimal scoreAfter,
            String reason,
            Long rentalId,
            LocalDateTime createdAt
    ) {
    }

    public record BackgroundCheckView(
            Long id,
            Long customerId,
            String customerName,
            String document,
            BackgroundCheckStatus status,
            String result,
            String observations,
            String provider,
            boolean hasEvidence,
            LocalDateTime checkedAt
    ) {
    }

    public record BackgroundCheckRequest(
            @NotBlank(message = "El documento es obligatorio") String document,
            Long customerId
    ) {
    }

    public record ResolveBackgroundCheckRequest(
            @NotNull(message = "El resultado es obligatorio") BackgroundCheckStatus status,
            String result,
            String observations
    ) {
    }

    /**
     * Ficha completa del cliente: lo que se necesita para decidir en el
     * mostrador si se le alquila y bajo que condiciones.
     */
    public record CustomerProfile(
            CustomerView customer,
            List<RentalSummary> activeRentals,
            List<RentalSummary> finishedRentals,
            List<HeldItem> itemsInPossession,
            List<DebtSummary> openDebts,
            BigDecimal totalDebt,
            BigDecimal availableCredit,
            BigDecimal lifetimeValue,
            List<ReputationEventView> reputationHistory,
            List<BackgroundCheckView> backgroundChecks,
            List<DocumentSummary> documents
    ) {
    }

    public record RentalSummary(
            Long id,
            String code,
            String status,
            LocalDate startDate,
            LocalDate endDate,
            LocalDate actualReturnDate,
            BigDecimal amount,
            Integer itemCount,
            String deliveryAddress,
            boolean overdue
    ) {
    }

    /** Material que el cliente tiene en su poder en este momento. */
    public record HeldItem(Long productId, String productName, Integer quantity, Long rentalId,
                           String rentalCode, LocalDate dueDate, boolean overdue) {
    }

    public record DebtSummary(Long id, Long rentalId, String rentalCode, BigDecimal balance,
                              LocalDate dueDate, Integer overdueDays, String status) {
    }

    public record DocumentSummary(Long id, String documentType, String serialNumber, Long rentalId,
                                  LocalDateTime generatedAt) {
    }
}
