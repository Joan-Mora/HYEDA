package co.edu.uniminuto.hyeda.modules.clientes.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "background_check")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BackgroundCheck {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(nullable = false, length = 40)
    private String document;

    @Column(columnDefinition = "TEXT")
    private String result;

    @Column(length = 80)
    private String provider;

    @Column(name = "evidence_path", length = 500)
    private String evidencePath;

    @Column(name = "checked_at")
    private Instant checkedAt;

    @PrePersist
    void onCreate() {
        if (checkedAt == null) checkedAt = Instant.now();
    }
}
