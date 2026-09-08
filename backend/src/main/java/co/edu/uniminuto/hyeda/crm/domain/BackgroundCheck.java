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

import java.time.LocalDateTime;

@Entity
@Table(name = "background_check")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackgroundCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Puede quedar nulo: se permite consultar una cedula suelta antes de registrar al cliente. */
    @Column(name = "customer_id")
    private Long customerId;

    @Column(nullable = false, length = 40)
    private String document;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private BackgroundCheckStatus status = BackgroundCheckStatus.PENDIENTE_MANUAL;

    @Column(length = 4000)
    private String result;

    @Column(length = 4000)
    private String observations;

    @Column(nullable = false, length = 80)
    @Builder.Default
    private String provider = "manual";

    /** PDF del certificado adjuntado por el operador. */
    @Column(name = "evidence_path", length = 500)
    private String evidencePath;

    @Column(name = "checked_by")
    private Long checkedBy;

    @Column(name = "checked_at", nullable = false)
    @Builder.Default
    private LocalDateTime checkedAt = LocalDateTime.now();
}
