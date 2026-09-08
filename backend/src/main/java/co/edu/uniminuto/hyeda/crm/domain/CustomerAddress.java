package co.edu.uniminuto.hyeda.crm.domain;

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
 * Direccion guardada del cliente. Permite repetir una obra frecuente sin
 * volver a escribirla.
 */
@Entity
@Table(name = "customer_address")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** OBRA, RESIDENCIA u OFICINA. */
    @Column(nullable = false, length = 40)
    @Builder.Default
    private String label = "OBRA";

    @Column(nullable = false, length = 500)
    private String address;

    /** Como la nombra el cliente: "la obra del puente", "finca de don Jose". */
    @Column(length = 200)
    private String reference;

    private Double lat;

    private Double lng;

    @Column(name = "maps_url", length = 700)
    private String mapsUrl;

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private Boolean isDefault = false;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
