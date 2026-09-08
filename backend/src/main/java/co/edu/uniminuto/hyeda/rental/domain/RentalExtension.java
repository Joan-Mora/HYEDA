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

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Ampliacion del plazo pactada con el cliente. Corre la fecha de devolucion, de
 * modo que esos dias se cobran a tarifa normal y no como mora.
 */
@Entity
@Table(name = "rental_extension")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RentalExtension {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rental_id", nullable = false)
    private Long rentalId;

    @Column(name = "extra_days", nullable = false)
    private Integer extraDays;

    @Column(name = "previous_end", nullable = false)
    private LocalDate previousEnd;

    @Column(name = "new_end", nullable = false)
    private LocalDate newEnd;

    @Column(length = 400)
    private String reason;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "requested_at", nullable = false)
    @Builder.Default
    private LocalDateTime requestedAt = LocalDateTime.now();
}
