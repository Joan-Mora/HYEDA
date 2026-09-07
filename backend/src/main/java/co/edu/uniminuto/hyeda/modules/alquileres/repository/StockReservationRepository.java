package co.edu.uniminuto.hyeda.modules.alquileres.repository;

import co.edu.uniminuto.hyeda.modules.alquileres.entity.StockReservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {
    List<StockReservation> findByRentalIdAndConsumedFalse(Long rentalId);
    List<StockReservation> findByExpiresAtBeforeAndConsumedFalse(java.time.Instant now);
}
