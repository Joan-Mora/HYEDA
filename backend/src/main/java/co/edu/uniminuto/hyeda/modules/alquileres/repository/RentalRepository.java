package co.edu.uniminuto.hyeda.modules.alquileres.repository;

import co.edu.uniminuto.hyeda.modules.alquileres.entity.Rental;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RentalRepository extends JpaRepository<Rental, Long> {
    List<Rental> findByCustomerId(Long customerId);
    List<Rental> findByStatus(co.edu.uniminuto.hyeda.modules.alquileres.entity.RentalStatus status);
    List<Rental> findByCreatedAtBetween(java.time.Instant from, java.time.Instant to);

    @Query("select distinct r from Rental r left join fetch r.customer")
    List<Rental> findAllWithDetails();
}
