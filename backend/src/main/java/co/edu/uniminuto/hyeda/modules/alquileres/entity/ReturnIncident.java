package co.edu.uniminuto.hyeda.modules.alquileres.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "return_incident")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReturnIncident {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "rental_id", nullable = false)
    private Rental rental;

    @Column(nullable = false, length = 120)
    private String reason;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "reported_at")
    private Instant reportedAt;

    @PrePersist
    void onCreate() {
        if (reportedAt == null) reportedAt = Instant.now();
    }
}
