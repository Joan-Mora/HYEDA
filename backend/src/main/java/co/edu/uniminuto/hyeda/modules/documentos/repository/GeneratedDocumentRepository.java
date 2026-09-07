package co.edu.uniminuto.hyeda.modules.documentos.repository;

import co.edu.uniminuto.hyeda.modules.documentos.entity.GeneratedDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GeneratedDocumentRepository extends JpaRepository<GeneratedDocument, Long> {
    List<GeneratedDocument> findByRentalId(Long rentalId);
    Optional<GeneratedDocument> findBySerialNumber(String serialNumber);
    long countByDocumentType(co.edu.uniminuto.hyeda.modules.documentos.entity.GeneratedDocumentType type);
}
