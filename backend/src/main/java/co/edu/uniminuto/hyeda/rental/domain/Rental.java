package co.edu.uniminuto.hyeda.rental.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rental")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Rental {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Consecutivo legible para el mostrador: ALQ-2026-00042. */
    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    /** Quien atendio el alquiler. */
    @Column(name = "user_id")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private RentalStatus status = RentalStatus.BORRADOR;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_policy", nullable = false, length = 20)
    @Builder.Default
    private DocumentPolicy documentPolicy = DocumentPolicy.CONTRATO;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /** Fecha pactada de devolucion; se mueve con cada extension aprobada. */
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "actual_return_date")
    private LocalDate actualReturnDate;

    @Column(name = "estimated_days", nullable = false)
    private Integer estimatedDays;

    @Column(name = "actual_days")
    private Integer actualDays;

    @Column(name = "delivery_address", nullable = false, length = 500)
    private String deliveryAddress;

    @Column(name = "delivery_reference", length = 200)
    private String deliveryReference;

    @Column(name = "delivery_lat")
    private Double deliveryLat;

    @Column(name = "delivery_lng")
    private Double deliveryLng;

    @Column(name = "delivery_maps_url", length = 700)
    private String deliveryMapsUrl;

    /** Servicio de acarreo, opcional y con valor libre (puede ser cero). */
    @Column(name = "hauling_enabled", nullable = false)
    @Builder.Default
    private Boolean haulingEnabled = false;

    @Column(name = "hauling_cost", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal haulingCost = BigDecimal.ZERO;

    @Column(name = "discount_amount", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    /** Obligatorio si hay descuento; tambien lo exige la base de datos. */
    @Column(name = "discount_reason", length = 400)
    private String discountReason;

    @Column(name = "items_subtotal", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal itemsSubtotal = BigDecimal.ZERO;

    /** Lo que se le dijo al cliente al abrir el alquiler. */
    @Column(name = "estimated_total", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal estimatedTotal = BigDecimal.ZERO;

    @Column(name = "late_fee_total", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal lateFeeTotal = BigDecimal.ZERO;

    @Column(name = "incident_charge_total", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal incidentChargeTotal = BigDecimal.ZERO;

    /** Cifra definitiva; solo existe despues de la devolucion. */
    @Column(name = "final_amount", precision = 14, scale = 2)
    private BigDecimal finalAmount;

    @Column(name = "credit_applied", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal creditApplied = BigDecimal.ZERO;

    @Column(name = "refund_amount", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal refundAmount = BigDecimal.ZERO;

    @Column(name = "data_consent_accepted", nullable = false)
    @Builder.Default
    private Boolean dataConsentAccepted = false;

    @Column(name = "signature_path", length = 500)
    private String signaturePath;

    @Column(length = 4000)
    private String notes;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "rental_id")
    @Builder.Default
    private List<RentalItem> items = new ArrayList<>();

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /** Cifra vigente: la final si ya se liquido, la estimada mientras tanto. */
    public BigDecimal currentAmount() {
        return finalAmount != null ? finalAmount : estimatedTotal;
    }

    /** Dias de atraso frente a la fecha pactada, contra hoy o contra la devolucion real. */
    public long overdueDays() {
        if (status.isTerminal() && actualReturnDate == null) {
            return 0;
        }
        LocalDate reference = actualReturnDate != null ? actualReturnDate : LocalDate.now();
        long days = ChronoUnit.DAYS.between(endDate, reference);
        return Math.max(0, days);
    }

    public boolean isOverdue() {
        return status.isMaterialOut() && LocalDate.now().isAfter(endDate);
    }
}
