package co.edu.uniminuto.hyeda.rental.repository;

import co.edu.uniminuto.hyeda.rental.domain.Rental;
import co.edu.uniminuto.hyeda.rental.domain.RentalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RentalRepository extends JpaRepository<Rental, Long> {

    Optional<Rental> findByCode(String code);

    List<Rental> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    List<Rental> findByStatusIn(Collection<RentalStatus> statuses);

    @Query("""
            select r from Rental r
            where (:status is null or r.status = :status)
              and (:customerId is null or r.customerId = :customerId)
              and (:from is null or r.startDate >= :from)
              and (:to is null or r.startDate <= :to)
            """)
    Page<Rental> search(@Param("status") RentalStatus status,
                        @Param("customerId") Long customerId,
                        @Param("from") LocalDate from,
                        @Param("to") LocalDate to,
                        Pageable pageable);

    /** Material que sigue en la calle y ya paso de la fecha pactada. */
    @Query("select r from Rental r where r.status = 'EN_CURSO' and r.endDate < :today order by r.endDate asc")
    List<Rental> findOverdue(@Param("today") LocalDate today);

    @Query("select r from Rental r where r.status = 'EN_CURSO' order by r.endDate asc")
    List<Rental> findActive();

    /** El consecutivo se calcula por ano para que el codigo se reinicie cada enero. */
    @Query("select count(r) from Rental r where r.createdAt >= :from and r.createdAt < :to")
    long countCreatedBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select r from Rental r where r.createdAt between :from and :to order by r.createdAt asc")
    List<Rental> findCreatedBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select r from Rental r where r.closedAt between :from and :to order by r.closedAt asc")
    List<Rental> findClosedBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("""
            select coalesce(sum(coalesce(r.finalAmount, r.estimatedTotal)), 0) from Rental r
            where r.customerId = :customerId and r.status = 'CERRADO'
            """)
    BigDecimal lifetimeValue(@Param("customerId") Long customerId);

    long countByCustomerIdAndStatus(Long customerId, RentalStatus status);
}
