package co.edu.uniminuto.hyeda.documents.repository;

import co.edu.uniminuto.hyeda.documents.domain.DocumentSequence;
import co.edu.uniminuto.hyeda.documents.domain.GeneratedDocumentType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DocumentSequenceRepository extends JpaRepository<DocumentSequence, GeneratedDocumentType> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from DocumentSequence s where s.documentType = :type")
    Optional<DocumentSequence> findForUpdate(@Param("type") GeneratedDocumentType type);
}
