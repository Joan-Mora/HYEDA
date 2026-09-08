package co.edu.uniminuto.hyeda.billing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rental_id", nullable = false)
    private Long rentalId;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false, length = 20)
    private PaymentType paymentType;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    /** Efectivo entregado por el cliente; sirve para calcular el vuelto. */
    @Column(name = "amount_tendered", precision = 14, scale = 2)
    private BigDecimal amountTendered;

    @Column(name = "change_given", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal changeGiven = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDIENTE;

    @Column(length = 120)
    private String reference;

    @Column(length = 40)
    private String provider;

    @Column(name = "provider_payload", length = 4000)
    private String providerPayload;

    /** Contenido codificado en el QR entregado al cliente. */
    @Column(name = "qr_content", length = 1000)
    private String qrContent;

    @Column(length = 400)
    private String note;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "recorded_by")
    private Long recordedBy;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public boolean isConfirmed() {
        return status == PaymentStatus.CONFIRMADO;
    }
}
