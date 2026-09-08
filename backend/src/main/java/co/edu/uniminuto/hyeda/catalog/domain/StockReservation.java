package co.edu.uniminuto.hyeda.catalog.domain;

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

import java.time.LocalDateTime;

/**
 * Apartado temporal de unidades mientras se completa el alquiler.
 *
 * <p>Resuelve el caso de dos alquileres simultaneos sobre la ultima unidad:
 * el primero que llega descuenta el disponible y se lleva la unidad; el segundo
 * ve cero disponible. Si el primero no termina el flujo, la reserva expira y el
 * stock vuelve solo.
 */
@Entity
@Table(name = "stock_reservation")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "rental_id")
    private Long rentalId;

    /** Identificador del wizard en el navegador, para reservar antes de crear el alquiler. */
    @Column(name = "session_key", length = 80)
    private String sessionKey;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /** El material ya salio: la reserva se convirtio en despacho. */
    @Column(nullable = false)
    @Builder.Default
    private Boolean consumed = false;

    /** La reserva se deshizo y el stock volvio al patio. */
    @Column(nullable = false)
    @Builder.Default
    private Boolean released = false;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public boolean isActive() {
        return !Boolean.TRUE.equals(consumed) && !Boolean.TRUE.equals(released);
    }
}
