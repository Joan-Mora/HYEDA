package co.edu.uniminuto.hyeda.modules.clientes.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "customer")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String document;

    @Column(nullable = false, length = 180)
    private String name;

    @Column(length = 40)
    private String phone;

    @Column(length = 120)
    private String email;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(name = "id_copy_path", length = 500)
    private String idCopyPath;

    @Column(name = "data_consent", nullable = false)
    @Builder.Default
    private Boolean dataConsent = false;

    @Column(name = "reputation_score", precision = 8, scale = 2)
    @Builder.Default
    private BigDecimal reputationScore = BigDecimal.valueOf(50);

    @Enumerated(EnumType.STRING)
    @Column(name = "customer_type", length = 20)
    @Builder.Default
    private CustomerType customerType = CustomerType.NUEVO;

    @Column(name = "created_at")
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (dataConsent == null) dataConsent = false;
        if (reputationScore == null) reputationScore = BigDecimal.valueOf(50);
        if (customerType == null) customerType = CustomerType.NUEVO;
    }
}
