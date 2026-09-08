package co.edu.uniminuto.hyeda.rental.repository;

import co.edu.uniminuto.hyeda.rental.domain.RentalExtension;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RentalExtensionRepository extends JpaRepository<RentalExtension, Long> {

    List<RentalExtension> findByRentalIdOrderByRequestedAtAsc(Long rentalId);
}
