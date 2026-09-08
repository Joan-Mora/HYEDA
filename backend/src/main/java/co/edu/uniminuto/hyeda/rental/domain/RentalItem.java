package co.edu.uniminuto.hyeda.rental.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Entity
@Table(name = "rental_item")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RentalItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * La coleccion del alquiler es quien escribe esta columna; aqui se expone
     * solo para poder consultar items por alquiler sin cargar el agregado.
     */
    @Column(name = "rental_id")
    private Long rentalId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    /**
     * Nombre congelado al momento del alquiler: si despues renombran el equipo,
     * el contrato y la factura siguen diciendo lo que se entrego.
     */
    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "quantity_returned", nullable = false)
    @Builder.Default
    private Integer quantityReturned = 0;

    /** Precio dia congelado; una lista de precios nueva no altera lo pactado. */
    @Column(name = "unit_price", nullable = false, precision = 14, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "late_fee_snapshot", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal lateFeeSnapshot = BigDecimal.ZERO;

    @Column(name = "estimated_days", nullable = false)
    private Integer estimatedDays;

    @Column(name = "actual_days")
    private Integer actualDays;

    @Column(name = "line_estimated", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal lineEstimated = BigDecimal.ZERO;

    @Column(name = "line_final", precision = 14, scale = 2)
    private BigDecimal lineFinal;

    public int pendingReturn() {
        return quantity - quantityReturned;
    }
}
