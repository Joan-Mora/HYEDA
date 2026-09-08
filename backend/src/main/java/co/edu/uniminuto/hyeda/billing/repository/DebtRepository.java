package co.edu.uniminuto.hyeda.billing.repository;

import co.edu.uniminuto.hyeda.billing.domain.Debt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface DebtRepository extends JpaRepository<Debt, Long> {

    Optional<Debt> findByRentalId(Long rentalId);

    @Query("select d from Debt d where d.customerId = :customerId and d.status in ('ABIERTA','PARCIAL','VENCIDA')")
    List<Debt> findOpenByCustomer(@Param("customerId") Long customerId);

    @Query("select d from Debt d where d.status in ('ABIERTA','PARCIAL','VENCIDA') order by d.overdueDays desc")
    List<Debt> findAllOpen();

    @Query("select d from Debt d where d.status in ('ABIERTA','PARCIAL','VENCIDA')")
    Page<Debt> findOpenPaged(Pageable pageable);

    @Query("""
            select coalesce(sum(d.balance), 0) from Debt d
            where d.customerId = :customerId and d.status in ('ABIERTA','PARCIAL','VENCIDA')
            """)
    BigDecimal openBalanceForCustomer(@Param("customerId") Long customerId);

    @Query("select coalesce(sum(d.balance), 0) from Debt d where d.status in ('ABIERTA','PARCIAL','VENCIDA')")
    BigDecimal totalOpenBalance();

    /** Morosos que llevan mas de N dias vencidos; alimenta el seguimiento por WhatsApp. */
    @Query("""
            select d from Debt d
            where d.status in ('ABIERTA','PARCIAL','VENCIDA') and d.overdueDays >= :minDays
            order by d.overdueDays desc
            """)
    List<Debt> findOverdueBeyond(@Param("minDays") int minDays);
}
