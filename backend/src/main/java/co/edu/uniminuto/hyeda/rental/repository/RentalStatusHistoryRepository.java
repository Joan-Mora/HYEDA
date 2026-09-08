package co.edu.uniminuto.hyeda.rental.repository;

import co.edu.uniminuto.hyeda.rental.domain.RentalStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RentalStatusHistoryRepository extends JpaRepository<RentalStatusHistory, Long> {

    List<RentalStatusHistory> findByRentalIdOrderByChangedAtAsc(Long rentalId);
}
