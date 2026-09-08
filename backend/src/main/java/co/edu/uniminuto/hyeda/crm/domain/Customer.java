package co.edu.uniminuto.hyeda.crm.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "customer")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String document;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 20)
    @Builder.Default
    private IdDocumentType documentType = IdDocumentType.CC;

    @Column(nullable = false, length = 180)
    private String name;

    @Column(length = 40)
    private String phone;

    @Column(length = 120)
    private String email;

    /**
     * Copia de la cedula. Se guarda una sola vez: si el cliente vuelve, no hay
     * que pedirsela de nuevo.
     */
    @Column(name = "id_copy_path", length = 500)
    private String idCopyPath;

    @Column(name = "data_consent", nullable = false)
    @Builder.Default
    private Boolean dataConsent = false;

    @Column(name = "data_consent_at")
    private LocalDateTime dataConsentAt;

    @Column(name = "reputation_score", nullable = false, precision = 8, scale = 2)
    @Builder.Default
    private BigDecimal reputationScore = new BigDecimal("50.00");

    @Enumerated(EnumType.STRING)
    @Column(name = "customer_type", nullable = false, length = 20)
    @Builder.Default
    private CustomerType customerType = CustomerType.NUEVO;

    /** Alquileres cerrados con exito; alimenta la clasificacion del cliente. */
    @Column(name = "rental_count", nullable = false)
    @Builder.Default
    private Integer rentalCount = 0;

    @Column(length = 4000)
    private String notes;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    @OrderBy("isDefault desc, id asc")
    @Builder.Default
    private List<CustomerAddress> addresses = new ArrayList<>();

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public boolean hasIdCopy() {
        return idCopyPath != null && !idCopyPath.isBlank();
    }

    /** Solo un cliente de confianza puede saltarse el contrato de arrendamiento. */
    public boolean canSkipContract() {
        return customerType == CustomerType.CONFIANZA;
    }
}
