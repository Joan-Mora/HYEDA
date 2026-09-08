package co.edu.uniminuto.hyeda.catalog.repository;

import co.edu.uniminuto.hyeda.catalog.domain.StockReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {

    @Query("select r from StockReservation r where r.rentalId = :rentalId "
            + "and r.consumed = false and r.released = false")
    List<StockReservation> findActiveByRental(@Param("rentalId") Long rentalId);

    @Query("select r from StockReservation r where r.sessionKey = :sessionKey "
            + "and r.consumed = false and r.released = false")
    List<StockReservation> findActiveBySession(@Param("sessionKey") String sessionKey);

    /** Reservas vencidas que aun retienen stock; el job programado las libera. */
    @Query("select r from StockReservation r where r.consumed = false and r.released = false "
            + "and r.expiresAt < :now order by r.createdAt asc")
    List<StockReservation> findExpired(@Param("now") LocalDateTime now);
}
