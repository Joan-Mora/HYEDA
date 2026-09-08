package co.edu.uniminuto.hyeda.catalog.repository;

import co.edu.uniminuto.hyeda.catalog.domain.StockLedgerEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface StockLedgerRepository extends JpaRepository<StockLedgerEntry, Long> {

    Page<StockLedgerEntry> findByProductIdOrderByCreatedAtDesc(Long productId, Pageable pageable);

    List<StockLedgerEntry> findByRentalIdOrderByCreatedAtAsc(Long rentalId);

    /**
     * Productos que llegaron a cero disponible dentro del rango, para la
     * metrica de quiebres de stock.
     */
    @Query("""
            select distinct e.productId from StockLedgerEntry e
            where e.balanceAfter = 0 and e.createdAt between :from and :to
            """)
    List<Long> findProductIdsThatHitZero(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
