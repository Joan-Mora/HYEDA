package co.edu.uniminuto.hyeda.crm.domain;

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
 * Cada cambio de reputacion queda registrado con su motivo, de modo que el
 * puntaje siempre se puede explicar frente al cliente.
 */
@Entity
@Table(name = "reputation_event")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReputationEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "rental_id")
    private Long rentalId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 40)
    private ReputationEventType eventType;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal delta;

    @Column(name = "score_after", nullable = false, precision = 8, scale = 2)
    private BigDecimal scoreAfter;

    @Column(length = 400)
    private String reason;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
