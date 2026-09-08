package co.edu.uniminuto.hyeda.documents.domain;

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
@Table(name = "generated_document")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rental_id")
    private Long rentalId;

    @Column(name = "customer_id")
    private Long customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 20)
    private GeneratedDocumentType documentType;

    /** Consecutivo legal impreso en el documento: CTR-00001. */
    @Column(name = "serial_number", nullable = false, unique = true, length = 40)
    private String serialNumber;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(length = 200)
    private String title;

    @Column(name = "metadata_json", length = 4000)
    private String metadataJson;

    @Column(nullable = false)
    @Builder.Default
    private Boolean signed = false;

    @Column(name = "signed_at")
    private LocalDateTime signedAt;

    @Column(name = "generated_by")
    private Long generatedBy;

    @Column(name = "generated_at", nullable = false)
    @Builder.Default
    private LocalDateTime generatedAt = LocalDateTime.now();
}
