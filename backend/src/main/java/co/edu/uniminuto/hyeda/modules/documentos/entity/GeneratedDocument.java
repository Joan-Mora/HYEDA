package co.edu.uniminuto.hyeda.modules.documentos.entity;

import co.edu.uniminuto.hyeda.modules.alquileres.entity.Rental;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "generated_document")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeneratedDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rental_id")
    private Rental rental;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 30)
    private GeneratedDocumentType documentType;

    @Column(name = "serial_number", nullable = false, unique = true, length = 40)
    private String serialNumber;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    @Column(name = "generated_at")
    private Instant generatedAt;

    @PrePersist
    void onCreate() {
        if (generatedAt == null) generatedAt = Instant.now();
    }
}
