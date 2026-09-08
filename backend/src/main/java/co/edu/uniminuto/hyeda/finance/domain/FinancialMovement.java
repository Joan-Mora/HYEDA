package co.edu.uniminuto.hyeda.finance.domain;

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
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Libro de caja de la empresa: cada peso que entra o sale.
 *
 * <p>Los pagos de alquiler se concilian aqui automaticamente, y los gastos
 * (combustible, mantenimiento, nomina) se cargan a mano o por importacion del
 * Excel que la administradora venia llevando.
 */
@Entity
@Table(name = "financial_movement")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 20)
    private MovementType movementType;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(length = 400)
    private String description;

    @Column(length = 80)
    private String category;

    @Column(name = "movement_date", nullable = false)
    private LocalDate movementDate;

    @Column(name = "rental_id")
    private Long rentalId;

    /** Presente cuando el movimiento nace de un pago; evita conciliar dos veces. */
    @Column(name = "payment_id")
    private Long paymentId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /** Signo contable: los gastos restan. */
    public BigDecimal signedAmount() {
        return movementType == MovementType.GASTO ? amount.negate() : amount;
    }
}
