package co.edu.uniminuto.hyeda.modules.documentos.dto;

import co.edu.uniminuto.hyeda.modules.documentos.entity.GeneratedDocumentType;

import java.time.Instant;

public record DocumentDto(
        Long id,
        Long rentalId,
        GeneratedDocumentType documentType,
        String serialNumber,
        String filePath,
        Instant generatedAt
) {
}
