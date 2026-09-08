package co.edu.uniminuto.hyeda.rental.repository;

import co.edu.uniminuto.hyeda.rental.domain.RentalItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface RentalItemRepository extends JpaRepository<RentalItem, Long> {

    List<RentalItem> findByRentalId(Long rentalId);

    /**
     * Ranking de equipos mas alquilados en un rango, por unidades despachadas
     * y por dinero facturado.
     *
     * @return filas {@code [productId, productName, unidades, monto]}
     */
    @Query("""
            select i.productId, i.productName, sum(i.quantity),
                   coalesce(sum(coalesce(i.lineFinal, i.lineEstimated)), 0)
            from RentalItem i join Rental r on r.id = i.rentalId
            where r.createdAt between :from and :to and r.status <> 'CANCELADO'
            group by i.productId, i.productName
            order by sum(i.quantity) desc
            """)
    List<Object[]> rankProducts(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** Material que un cliente tiene en su poder ahora mismo. */
    @Query("""
            select i from RentalItem i join Rental r on r.id = i.rentalId
            where r.customerId = :customerId and r.status = 'EN_CURSO'
            """)
    List<RentalItem> findHeldByCustomer(@Param("customerId") Long customerId);
}
