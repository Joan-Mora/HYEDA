package co.edu.uniminuto.hyeda.rental.repository;

import co.edu.uniminuto.hyeda.rental.domain.ReturnIncident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReturnIncidentRepository extends JpaRepository<ReturnIncident, Long> {

    List<ReturnIncident> findByRentalIdOrderByReportedAtAsc(Long rentalId);

    List<ReturnIncident> findByReportedAtBetweenOrderByReportedAtDesc(LocalDateTime from, LocalDateTime to);

    @Query("""
            select i.itemCondition, count(i), coalesce(sum(i.chargeAmount), 0)
            from ReturnIncident i
            where i.reportedAt between :from and :to
            group by i.itemCondition
            """)
    List<Object[]> summarizeByCondition(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
