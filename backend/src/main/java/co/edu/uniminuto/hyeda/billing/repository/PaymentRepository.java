package co.edu.uniminuto.hyeda.billing.repository;

import co.edu.uniminuto.hyeda.billing.domain.Payment;
import co.edu.uniminuto.hyeda.billing.domain.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByRentalIdOrderByCreatedAtAsc(Long rentalId);

    Optional<Payment> findByReference(String reference);

    Page<Payment> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Dinero efectivamente recibido por un alquiler. Los reembolsos restan, de
     * modo que el resultado es siempre el neto a favor de la empresa.
     */
    @Query("""
            select coalesce(sum(case when p.paymentType = 'REEMBOLSO' then -p.amount else p.amount end), 0)
            from Payment p
            where p.rentalId = :rentalId and p.status = 'CONFIRMADO'
            """)
    BigDecimal netPaidForRental(@Param("rentalId") Long rentalId);

    List<Payment> findByStatusAndCreatedAtBefore(PaymentStatus status, LocalDateTime cutoff);

    @Query("""
            select p from Payment p
            where p.status = 'CONFIRMADO' and p.paidAt between :from and :to
            order by p.paidAt asc
            """)
    List<Payment> findConfirmedBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
