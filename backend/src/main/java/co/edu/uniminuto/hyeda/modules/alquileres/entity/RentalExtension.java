package co.edu.uniminuto.hyeda.modules.alquileres.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "rental_extension")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RentalExtension {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "rental_id", nullable = false)
    private Rental rental;

    @Column(name = "extra_days", nullable = false)
    private Integer extraDays;

    @Column(name = "requested_at")
    private Instant requestedAt;

    @PrePersist
    void onCreate() {
        if (requestedAt == null) requestedAt = Instant.now();
    }
}
