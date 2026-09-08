package co.edu.uniminuto.hyeda.documents.repository;

import co.edu.uniminuto.hyeda.documents.domain.GeneratedDocument;
import co.edu.uniminuto.hyeda.documents.domain.GeneratedDocumentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface GeneratedDocumentRepository extends JpaRepository<GeneratedDocument, Long> {

    Optional<GeneratedDocument> findBySerialNumber(String serialNumber);

    List<GeneratedDocument> findByRentalIdOrderByGeneratedAtDesc(Long rentalId);

    List<GeneratedDocument> findByCustomerIdOrderByGeneratedAtDesc(Long customerId);

    Optional<GeneratedDocument> findFirstByRentalIdAndDocumentTypeOrderByGeneratedAtDesc(
            Long rentalId, GeneratedDocumentType documentType);

    /**
     * Busqueda de remisiones y contratos por numero o por nombre del cliente,
     * con filtros de tipo y rango de fechas.
     */
    @Query("""
            select d from GeneratedDocument d
            where (:type is null or d.documentType = :type)
              and (:from is null or d.generatedAt >= :from)
              and (:to is null or d.generatedAt <= :to)
              and (:search is null
                   or lower(d.serialNumber) like lower(concat('%', :search, '%'))
                   or lower(coalesce(d.title, '')) like lower(concat('%', :search, '%'))
                   or d.customerId in (select c.id from Customer c
                                       where lower(c.name) like lower(concat('%', :search, '%'))
                                          or c.document like concat('%', :search, '%')))
            order by d.generatedAt desc
            """)
    Page<GeneratedDocument> search(@Param("search") String search,
                                   @Param("type") GeneratedDocumentType type,
                                   @Param("from") LocalDateTime from,
                                   @Param("to") LocalDateTime to,
                                   Pageable pageable);
}
