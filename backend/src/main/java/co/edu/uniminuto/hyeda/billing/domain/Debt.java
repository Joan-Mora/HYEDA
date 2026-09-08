package co.edu.uniminuto.hyeda.billing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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

/**
 * Saldo por cobrar a un cliente. Se abre cuando un alquiler se liquida sin
 * pago completo y alimenta la seccion de deudas pendientes.
 */
@Entity
@Table(name = "debt")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Debt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "rental_id")
    private Long rentalId;

    @Column(name = "original_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal originalAmount;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal balance;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "overdue_days", nullable = false)
    @Builder.Default
    private Integer overdueDays = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private DebtStatus status = DebtStatus.ABIERTA;

    /** Ultimo recordatorio enviado, para no molestar al cliente a diario. */
    @Column(name = "last_notice_at")
    private LocalDateTime lastNoticeAt;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isOpen() {
        return status == DebtStatus.ABIERTA || status == DebtStatus.PARCIAL || status == DebtStatus.VENCIDA;
    }
}
