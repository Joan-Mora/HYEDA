package co.edu.uniminuto.hyeda.documents.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Contador de consecutivos por tipo de documento. Se lee con bloqueo pesimista
 * para que dos operadores nunca obtengan el mismo numero de contrato.
 */
@Entity
@Table(name = "document_sequence")
@Getter
@Setter
@NoArgsConstructor
public class DocumentSequence {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", length = 20)
    private GeneratedDocumentType documentType;

    @Column(nullable = false, length = 10)
    private String prefix;

    @Column(name = "next_value", nullable = false)
    private Long nextValue;
}
