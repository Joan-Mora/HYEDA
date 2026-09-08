package co.edu.uniminuto.hyeda.documents.dto;

import co.edu.uniminuto.hyeda.documents.domain.GeneratedDocumentType;

import java.time.LocalDateTime;

public final class DocumentDtos {

    private DocumentDtos() {
    }

    /**
     * Fila del buscador de documentos. Incluye el nombre y la cedula del cliente
     * porque el mostrador busca las remisiones por persona, no por consecutivo.
     */
    public record DocumentRow(
            Long id,
            GeneratedDocumentType documentType,
            String documentTypeLabel,
            String serialNumber,
            String title,
            Long rentalId,
            String rentalCode,
            Long customerId,
            String customerName,
            String customerDocument,
            boolean signed,
            LocalDateTime generatedAt,
            String previewUrl,
            String downloadUrl
    ) {
    }

    /** Respuesta publica del QR impreso: confirma que el documento es autentico. */
    public record DocumentVerification(
            boolean valid,
            String serialNumber,
            String documentTypeLabel,
            String issuedTo,
            LocalDateTime generatedAt,
            String message
    ) {
    }
}
