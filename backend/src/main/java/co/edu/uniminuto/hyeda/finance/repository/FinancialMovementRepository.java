package co.edu.uniminuto.hyeda.finance.repository;

import co.edu.uniminuto.hyeda.finance.domain.FinancialMovement;
import co.edu.uniminuto.hyeda.finance.domain.MovementType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface FinancialMovementRepository extends JpaRepository<FinancialMovement, Long> {

    boolean existsByPaymentId(Long paymentId);

    List<FinancialMovement> findByPaymentId(Long paymentId);

    List<FinancialMovement> findByRentalIdOrderByMovementDateAsc(Long rentalId);

    @Query("""
            select m from FinancialMovement m
            where (:type is null or m.movementType = :type)
              and (:category is null or m.category = :category)
              and (:from is null or m.movementDate >= :from)
              and (:to is null or m.movementDate <= :to)
            order by m.movementDate desc, m.id desc
            """)
    Page<FinancialMovement> search(@Param("type") MovementType type,
                                   @Param("category") String category,
                                   @Param("from") LocalDate from,
                                   @Param("to") LocalDate to,
                                   Pageable pageable);

    @Query("""
            select coalesce(sum(m.amount), 0) from FinancialMovement m
            where m.movementType = :type and m.movementDate between :from and :to
            """)
    BigDecimal totalBetween(@Param("type") MovementType type,
                            @Param("from") LocalDate from,
                            @Param("to") LocalDate to);

    /** Gastos agrupados por categoria, para el grafico de la seccion de finanzas. */
    @Query("""
            select coalesce(m.category, 'Sin categoria'), sum(m.amount)
            from FinancialMovement m
            where m.movementType = :type and m.movementDate between :from and :to
            group by m.category
            order by sum(m.amount) desc
            """)
    List<Object[]> totalsByCategory(@Param("type") MovementType type,
                                    @Param("from") LocalDate from,
                                    @Param("to") LocalDate to);

    /** Serie diaria de ingresos y gastos para el grafico de evolucion. */
    @Query("""
            select m.movementDate, m.movementType, sum(m.amount)
            from FinancialMovement m
            where m.movementDate between :from and :to
            group by m.movementDate, m.movementType
            order by m.movementDate asc
            """)
    List<Object[]> dailyTotals(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("select distinct m.category from FinancialMovement m where m.category is not null order by m.category")
    List<String> categories();
}
