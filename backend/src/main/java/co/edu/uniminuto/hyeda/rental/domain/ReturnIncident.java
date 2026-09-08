package co.edu.uniminuto.hyeda.rental.domain;

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

/**
 * Novedad detectada al recibir el material: dano, faltante o inconformidad.
 * Es la fuente de la metrica de devoluciones problematicas.
 */
@Entity
@Table(name = "return_incident")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnIncident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rental_id", nullable = false)
    private Long rentalId;

    @Column(name = "rental_item_id")
    private Long rentalItemId;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_condition", nullable = false, length = 30)
    private IncidentCondition itemCondition;

    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    @Column(nullable = false, length = 200)
    private String reason;

    @Column(length = 4000)
    private String description;

    @Column(name = "charge_amount", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal chargeAmount = BigDecimal.ZERO;

    @Column(name = "evidence_path", length = 500)
    private String evidencePath;

    @Column(name = "reported_by")
    private Long reportedBy;

    @Column(name = "reported_at", nullable = false)
    @Builder.Default
    private LocalDateTime reportedAt = LocalDateTime.now();
}
