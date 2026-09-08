package co.edu.uniminuto.hyeda.catalog.domain;

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

import java.time.LocalDateTime;

/**
 * Asiento del libro mayor de stock. Solo se inserta, nunca se actualiza:
 * es la explicacion auditable de cada cambio en el disponible de un producto.
 */
@Entity
@Table(name = "stock_ledger")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockLedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "rental_id")
    private Long rentalId;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 30)
    private StockMovementType movementType;

    /** Positivo cuando entra al patio, negativo cuando sale. */
    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "balance_after", nullable = false)
    private Integer balanceAfter;

    @Column(length = 400)
    private String note;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
